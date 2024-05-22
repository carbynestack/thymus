# Carbyne Stack Thymus Authentication and Authorization

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/233198c332f3486ea69057fb9938917e)](https://app.codacy.com/gh/carbynestack/caliper/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Known Vulnerabilities](https://snyk.io/test/github/carbynestack/thymus/badge.svg)](https://snyk.io/test/github/carbynestack/thymus)
[![pre-commit](https://img.shields.io/badge/pre--commit-enabled-brightgreen?logo=pre-commit&logoColor=white)](https://github.com/pre-commit/pre-commit)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md)

> **DISCLAIMER**: Carbyne Stack Thymus is in *proof-of-concept* stage. The
> software is not ready for production use. It has neither been developed nor
> tested for a specific use case.

Thymus is the authentication and authorization subsystem of
[Carbyne Stack](https://github.com/carbynestack).

## Namesake

> The *thymus* is an organ that is critically important to the immune system
> which serves as the body’s defense mechanism providing surveillance and
> protection against diverse pathogens, tumors, antigens and mediators of tissue
> damage. ([Source](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC6446584/))

Within Carbyne Stack *Thymus* implements measures and mechanisms to defend
against unauthorized access.

## Development

### Isolated Deployment

The Thymus subsystem can be run in isolation, i.e., without a full-fledged
Carbyne Stack system. In order to follow the steps below, we assume that you
have a `kind` Kubernetes cluster at your disposal that comes along with Istio,
MetalLB, and the Zalando PostgreSQL operator. This can be achieved by following
the
[Platform Setup Guide](https://carbynestack.io/documentation/getting-started/deployment/manual/platform-setup)
available on the [Carbyne Stack website](https://carbynestack.io).

> \[!TIP\] You can skip the Knative installation as it is not required for
> running Thymus.

To deploy Thymus follow the steps below:

1. Clone the Thymus repository:

   ```bash
   git clone https://github.com/carbynestack/thymus.git
   ```

1. Create the PostgreSQL instance required by both Kratos and Hydra:

   ```bash
   kubectl apply -f thymus/hack/postgres.yaml
   ```

1. Change into the Thymus chart directory:

   ```bash
   cd thymus/charts/thymus
   ```

1. Fetch the dependencies of the chart:

   ```bash
   helm dependency update
   ```

1. Install the chart:

   ```bash
   helm install thymus . --set thymus.gateway.enabled=true --set thymus.users.enabled=true
   ```

   > \[!NOTE\] `thymus.gateway.enabled=true` and `thymus.users.enabled=true` are
   > optional flags that enable the creation of an Istio gateway and a set of
   > demo users respectively.

Thymus is now available and exposes the following APIs at the given endpoints:

<!-- markdownlint-disable MD034 -->

| API                                                            | Endpoint                               |
| -------------------------------------------------------------- | -------------------------------------- |
| [Kratos](https://www.ory.sh/docs/kratos/reference/api)         | http://172.18.1.128.sslip.io/iam       |
| [Kratos UI](https://github.com/ory/kratos-selfservice-ui-node) | http://172.18.1.128.sslip.io/iam/ui    |
| [Hydra](https://www.ory.sh/docs/hydra/reference/api)           | http://172.18.1.128.sslip.io/iam/oauth |

<!-- markdownlint-enable MD034 -->

### Authentication Flow

> \[!NOTE\] The following assumes that you have deployed Thymus as described
> above.

The following steps demonstrate the OpenID Connect authentication flow using
Thymus:

1. Get the OAuth2 client ID:

   <!-- markdownlint-disable MD013 -->

   ```bash
   CLIENT_ID=$(kubectl get secrets thymus-client-secret -o jsonpath='{.data.CLIENT_ID}' | base64 -d)
   ```

   <!-- markdownlint-enable MD013 -->

1. Request an authorization code by opening the following URL in a browser and
   authenticate yourself via the credentials of one of the users listed in
   `charts/thymus/values.yaml`:

   ```bash
   open "http://172.18.1.128.sslip.io/iam/oauth/oauth2/auth?client_id=${CLIENT_ID}&redirect_uri=http%3A%2F%2F127.0.0.1%3A5555%2Fcallback&response_type=code&state=1102398157&scope=offline%20openid"
   ```

   After being redirected to address `http://127.0.0.1/callback` copy the value
   of the `code` query parameter and store it in the `$AUTH_CODE` variable.

   ```bash
   AUTH_CODE="<token>"
   ```

1. Exchange the authentication code for an authentication token:

   ```bash
   curl --request POST \
   --url http://172.18.1.128.sslip.io/iam/oauth/oauth2/token \
   --header 'Content-Type: application/x-www-form-urlencoded' \
   --data client_id=${CLIENT_ID} \
   --data code=${AUTH_CODE} \
   --data grant_type=authorization_code \
   --data redirect_uri=http://127.0.0.1:5555/callback
   ```

You can use the returned access and refresh tokens to authenticate yourself to
an Istio with properly configured
[End User Authentication](https://istio.io/latest/docs/tasks/security/authentication/authn-policy/#end-user-authentication).

## License

The Carbyne Stack *Thymus Authentication and Authorization* subsystem repository
is open-sourced under the Apache License 2.0. See the [LICENSE](LICENSE) file
for details.

### 3rd Party Licenses

For information on how license obligations for 3rd party OSS dependencies are
fulfilled see the [README](https://github.com/carbynestack/carbynestack) file of
the Carbyne Stack repository.

## Contributing

Please see the Carbyne Stack
[Contributor's Guide](https://github.com/carbynestack/carbynestack/blob/master/CONTRIBUTING.md)
