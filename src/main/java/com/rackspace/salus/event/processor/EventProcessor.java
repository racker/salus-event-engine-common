/*
 * Copyright 2020 Rackspace US, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rackspace.salus.event.processor;

import com.rackspace.salus.event.statemachines.MultiStateTransition;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.Comparator;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.ComparisonExpression;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.Expression;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.LogicalExpression;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.LogicalExpression.Operator;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.PercentageFunction;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.PreviousFunction;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.RateFunction;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.StateExpression;
import com.rackspace.salus.telemetry.entities.EventEngineTaskParameters.TaskState;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public class EventProcessor {

  private final static StateExpression FALLBACK_STATE_EXPR =
      new StateExpression().setState(TaskState.OK);

  /**
   * Used for interpolating field references in the configured message, such as
   * "The value #{idle} is too low".
   */
  private static final Pattern MSG_INTERPOLATE = Pattern.compile("#\\{([^}]+)}");
  private static final String MISSING_INTERPOLATION = "undefined";

  public void process(EventProcessorContext context, EventProcessorInput input,
                      StateChangeHandler stateChangeHandler) {
    final EventEngineTaskParameters taskParameters = context.getTask().getTaskParameters();

    final StateExpression result = taskParameters.getStateExpressions().stream()
        .filter(
            stateExpression -> evaluateExpression(context, input, stateExpression.getExpression())
        )
        .findFirst()
        .orElse(FALLBACK_STATE_EXPR);

    final MultiStateTransition<TaskState, String> transition = context.getStateMachine()
        .process(input.getZone(), result.getState());

    if (transition != null) {
      stateChangeHandler
          .handleStateChange(transition, interpolateMessage(
              result.getMessage(), input.getMetrics()));
    }
  }

  /**
   * Interpolates metric references as <code>#{metric-name}</code> in the given message template.
   * <p><b>NOTE</b> package-private for unit testing</p>
   */
  String interpolateMessage(String message, Map<String, Object> metrics) {
    if (message == null) {
      return null;
    }

    final Matcher matcher = MSG_INTERPOLATE.matcher(message);
    final StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      final String metricName = matcher.group(1);
      final Object value = metrics.get(metricName);
      matcher.appendReplacement(sb, value != null ? value.toString() : MISSING_INTERPOLATION);
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private boolean evaluateExpression(EventProcessorContext context, EventProcessorInput input,
                                     Expression expression) {
    if (expression instanceof LogicalExpression) {
      return evaluateLogicalExpression(context, input, ((LogicalExpression) expression));
    } else {
      return evaluateComparisonExpression(context, input, ((ComparisonExpression) expression));
    }
  }

  private boolean evaluateLogicalExpression(EventProcessorContext context,
                                            EventProcessorInput input,
                                            LogicalExpression expression) {
    try {
      if (expression.getOperator().equals(Operator.OR)) {
        return expression.getExpressions().stream()
            .anyMatch(subEx -> evaluateExpression(context, input, subEx));
      } else { // AND
        return expression.getExpressions().stream()
            .allMatch(subEx -> evaluateExpression(context, input, subEx));
      }
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Sub-expression of " + expression + " is not valid");
    }
  }

  private boolean evaluateComparisonExpression(EventProcessorContext context,
                                               EventProcessorInput input,
                                               ComparisonExpression expression) {
    if (expression.getComparisonValue() instanceof Number) {
      final Number comparisonValue;
      try {
        comparisonValue = (Number) expression.getComparisonValue();
      } catch (ClassCastException e) {
        throw new IllegalArgumentException(
            "Expression " + expression + " contains invalid numeric comparison value");
      }
      return evaluateNumericalComparison(
          resolveNumberInput(context, input, expression.getInput()),
          expression.getComparator(),
          comparisonValue
      );

    } else if (expression.getComparisonValue() instanceof String) {
      final String comparisonValue;
      try {
        comparisonValue = (String) expression.getComparisonValue();
      } catch (ClassCastException e) {
        throw new IllegalArgumentException(
            "Expression " + expression + " contains invalid string comparison value");
      }

      return evaluateStringComparison(
          resolveStringInput(context, input, expression.getInput()),
          expression.getComparator(),
          comparisonValue
      );
    } else {
      throw new IllegalArgumentException(
          "Expression " + expression + " contains invalid input type");
    }

  }

  private boolean evaluateNumericalComparison(@Nullable Number lhs, Comparator comparator,
                                              Number rhs) {
    if (lhs == null) {
      return false;
    }

    final float lhsFloat = lhs.floatValue();
    final float rhsFloat = rhs.floatValue();
    switch (comparator) {
      case GREATER_THAN:
        return lhsFloat > rhsFloat;
      case EQUAL_TO:
        return lhsFloat == rhsFloat;
      case GREATER_THAN_OR_EQUAL_TO:
        return lhsFloat >= rhsFloat;
      case LESS_THAN:
        return lhsFloat < rhsFloat;
      case LESS_THAN_OR_EQUAL_TO:
        return lhsFloat <= rhsFloat;
      case NOT_EQUAL_TO:
        return lhsFloat != rhsFloat;
      default:
        throw new IllegalArgumentException(
            "Invalid comparator " + comparator + " for numerical comparison");
    }
  }

  private Number resolveNumberInput(EventProcessorContext context,
                                    EventProcessorInput input,
                                    Object expressionInput) {
    if (expressionInput instanceof String) {
      return ((Number) input.getMetrics().get(expressionInput));
    } else if (expressionInput instanceof RateFunction) {
      return resolveRateInput(context, input, ((RateFunction) expressionInput));
    } else if (expressionInput instanceof PercentageFunction) {
      return resolvePercentageInput(context, input, ((PercentageFunction) expressionInput));
    } else if (expressionInput instanceof PreviousFunction) {
      return resolvePreviousInput(context, input, ((PreviousFunction) expressionInput));
    } else {
      throw new IllegalArgumentException("Invalid numerical input type: " + expressionInput);
    }
  }

  private Number resolvePreviousInput(EventProcessorContext context, EventProcessorInput input,
                                      PreviousFunction expressionInput) {
    final EventProcessorInput previousInput = context.getPreviousInput();

    if (previousInput == null) {
      return null;
    }

    return (Number) previousInput.getMetrics().get(expressionInput.getOf());
  }

  private Number resolvePercentageInput(EventProcessorContext context, EventProcessorInput input,
                                        PercentageFunction expressionInput) {
    final Number partValue = (Number) input.getMetrics().get(expressionInput.getPart());
    final Number wholeValue = (Number) input.getMetrics().get(expressionInput.getWhole());

    return 100.0 * (partValue.floatValue() / wholeValue.floatValue());
  }

  private Number resolveRateInput(EventProcessorContext context, EventProcessorInput currentInput,
                                  RateFunction expressionInput) {
    final EventProcessorInput previousInput = context.getPreviousInput();

    if (previousInput == null) {
      return null;
    }

    final Number currentValue = (Number) currentInput.getMetrics().get(expressionInput.getOf());
    final Number previousValue = (Number) previousInput.getMetrics().get(expressionInput.getOf());
    if (currentValue == null || previousValue == null) {
      return null;
    }

    final long deltaSeconds = Duration.between(
        previousInput.getTimestamp(),
        currentInput.getTimestamp()
    ).getSeconds();

    return (currentValue.floatValue() - previousValue.floatValue()) / deltaSeconds;
  }

  private boolean evaluateStringComparison(String lhs, Comparator comparator, String rhs) {
    if (lhs == null) {
      return false;
    }

    switch (comparator) {
      case EQUAL_TO:
        return lhs.equals(rhs);
      case NOT_EQUAL_TO:
        return !lhs.equals(rhs);
      case REGEX_MATCH:
        return Pattern.compile(rhs).matcher(lhs).find();
      case NOT_REGEX_MATCH:
        return !Pattern.compile(rhs).matcher(lhs).find();
      default:
        throw new IllegalArgumentException(
            "Invalid comparator " + comparator + " for string comparison");
    }
  }

  private String resolveStringInput(EventProcessorContext context,
                                    EventProcessorInput input,
                                    Object expressionInput) {
    if (expressionInput instanceof String) {
      return (String) input.getMetrics().get(expressionInput);
    } else {
      throw new IllegalArgumentException("Invalid string input type: " + expressionInput);
    }
  }
}
