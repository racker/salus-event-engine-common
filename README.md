This module provides common services for the Event Engine subsystem. 

Primarily it provides the `EventEnginePicker` declared in the `discovery` package. The
`EventEnginePicker` or more generally the discovery services provides a configurable means
to determine Kapacitor instances for routing and addressing using a consistent hashing of
the measurement and event task attributes, such as tenant and resource ID.

## Kubernetes discovery configuration

The Event Engine applications have an option to use the Kubernetes API to discover the endpoints
for the Kapacitor instances to utilize for ingest and management. This section describes the
high level points that need to be addressed and configured for that discovery to work.

The Kapacitor deployment needs a headless service configured with
```yaml
type: ClusterIP
clusterIP: None
```

Environment variables of Event Engine Management and Ingest pods need to have
- `EVENT_DISCOVERY_KUBERNETESSTRATEGY_APIURL`: typically `https://kubernetes` for in-cluster access
- `EVENT_DISCOVERY_KUBERNETESSTRATEGY_SERVICENAME`: the name of the kapacitor headless service
  described above

The Event Engine application pods (ingest and management) will need a 
`serviceAccountName` assigned that has the following role:

```yaml
kind: Role
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  namespace: salus-development
  name: endpoint-watcher
rules:
  - apiGroups: [""] # "" indicates the core API group
    resources:
      - services
      - endpoints
    verbs: ["get", "list", "watch"]
```

## Example Kubernetes deployment

For testing of the `KubernetesServiceEndpointPicker`, an example Kubernetes manifest file is
provided at `examples/kube-deployment/kapacitor.yml`. It is expected to use this manifest with
a local cluster running in Docker for Desktop. 

The kapacitor statefulset and service can be deployed using:

```
kubectl apply -f kapacitor.yml
```