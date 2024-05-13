# Running the service locally using run-local.sh
This will run the service locally. It starts the database and localstack containers then start the service via a bash script.

## Environment variables

The script expects the following environment variables to be set:

```
HMPPS_CSIP_MANAGE_USERS_CLIENT_ID
HMPPS_CSIP_MANAGE_USERS_CLIENT_SECRET
HMPPS_CSIP_PRISONER_SEARCH_CLIENT_ID
HMPPS_CSIP_PRISONER_SEARCH_CLIENT_SECRET
```

These environment variables should be set to the dev secrets values. Remember to escape any `$` characters with `\$`.

## Running the service locally

Run the following commands from the root directory of the project:

1. docker compose -f docker-compose-local.yml pull
2. docker compose -f docker-compose-local.yml up 
3. ./run-local.sh
