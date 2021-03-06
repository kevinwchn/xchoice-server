## Introduction

X-Choice is a survey platform which can allow users to publish surveys quickly.

This package is the backend service for the application. For an overview of the project and the front end package, please check [xchoice-web](https://github.com/kevinwchn/xchoice-web).
## Tech stack

- Java
- Spring Boot & Spring Security
- JPA & Hibernate
- JUnit 5
- Mockito
- MySQL

## DB Design
### ER Diagrams
If we require survey takers to log in to take the survey (in other words survey can only be taken by authed users), here's the ER diagram:
![](designs/ER_diagram_taker_need_auth.png)
In this model, taker can be the publisher and vise versa.

If we consider survey taker to be anyone (including the audience who don't log in), here is the ER diagram:
![](designs/ER_diagram_taker_no_need_auth.png)
In this model taker and publisher are modeled separately. For simplicity, we didn't even model taker in the DB.

Comparing those two models, I picked the latter one since most of the survey apps can collect responses from public. Forcing them to log in can increase friction. And we are not providing additional values if they log in. Based on future needs, we can re-evaluate the need to modeling authed taker.

### Survey State Change
At a given time, a survey can be in `DRAFT`, `PUBLISHED`, `UNPUBLISHED`, `CANCELED` states. Those states are modeled and persisted in DB and surfaced to end user through API. The movements in the state machine are validated in API layer to keep DB query simple.
![](designs/survey_state_change.png)



| State | Behavior to survey participant | Behavior to publisher |
| ---- | ---- | --- |
| `DRAFT` | invisible | visible and editable |
| `PUBLISHED` | visible and workable | visible but not editable |
| `UNPUBLISHED` | invisible | visible and editable |
| `DELETED` | invisible | invisible |

## API Design

### Models
Survey
```json
{
  "id": 3,
  "title": "st",
  "questions": [
    {
      "id": 4,
      "title": "qt",
      "choices":[
        {
          "id": 5,
          "text": "ct",
          "responses": [
            {
              "id": 6,
              "slug": "asdklfnwek"
            }
          ]
        }
      ]
    }
  ]
}
```

SurveyMetadata
```json
{
  "surveyId": 3,
  "title": "st",
  "responses": 164,
  "status": "PUBLISHED"
}
```

| Resource | HTTP Method | Is public API? | Input | Output | Business Logic |
| ---- | ---- | --- | --- | --- | --- |
| `/surveys` | `POST` | No | Survey object with no ids and responses | Survey object with ids but no responses | Create survey |
| `/surveys` | `GET` | No | Empty | a List of SurveyMetadata | Find surveys published by user for displaying dashboard |
| `/surveys/{id}/responses` | `POST` | Yes | 3 (id), `[3,1,0]`. The second param is the selections. ex. the `3` at index 0 denotes for question 1 (0+1), customer selected the 4th option (3+1). | slug (ex. `ladksfadkjs`). Slug is a short string which can be used to come back to the selection. | Capture survey taker's selection |
| `/surveys/responses` | `GET` | Yes | slug | `["surveyId": 3, selections: "[3,1,0]"]` | Get the response. This is for displaying the taker the previous selections he/she made
| `/surveys/{id}` | `POST` | No | 3 (id), "PUBLISHED" (target status) | Empty | Update a survey to target status. The logic has check to make sure the transition follows the state machine.
| `/surveys/{id}` | `GET` | Yes | 3 (id) | Survey object | Get a given survey for taker to take
| `/surveys/{id}` | `DELETE` | No | 3 (id) | Empty | Delete a survey. This API was not implemented. Instead, we use the update survey status API to perform a soft delete for now: survey will be marked as `DELETED` but not removed from our DB.
| `/users` | `POST` | Yes | Empty. Because we can read the info from JWT token. | Empty | Put user to DB right after user logs int. This is to make sure the user id is created in our DB.

## Authentication
For private APIs, UI will request an access token from Auth0 and send to backend through HTTP request header as a bearer token. We use Spring Security 5 to validate the header of the token to make sure it is valid. For manual validation, we can go to https://jwt.io/ and check the access token.

For most of our usecases we need to understand which customer it is. The reason we don't let client pass the email id is that the email id can be faked. The way we get email id is by configuring an [Auth0 rule](https://auth0.com/docs/rules) to pass email id to access token. By using this we can make sure the email we get is secure and unmodified.

Then the server will call DB to get user id from the user email and perform other actions.

## Prod deployment
The Spring Boot app is being hosted on [Heroku](https://www.heroku.com/) with a ClearDB add-on for our MySQL. The endpoint is: https://x-choice-server.herokuapp.com/

Heroku tracks the github changes in the repository and deploys automatically.

Please note the plan I selected is a free plan, thus it has code start issue. If the app has not be interacted for a while, the next call will have a long latency.

## Local development

### Run dev server

I highly recommend using IntelliJ for Java development, which is the most powerful IDE for Java in my opinion. 

First, import project to IntelliJ by selecting `pom.xml` file and import as project. IntelliJ will automatically download and index dependencies for you.

To run the development server:

Go to `src/main/java/XChoiceApplication` and run the main function. By default, it will run on port 8080.

Since this service calls MySQL database, to avoid exposing the database login credentials, I've taken the approach to use env variables. Thus to run locally, you need the following env variables: `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Those can be configured in your `.bash_profile` or `.zshrc` depending on which one your desktop is using. Because of the same security concern, I will not post the names here, thus please drop me a message or email kangxu.wang@outlook.com for variable details.

### Run unit tests

In IntelliJ, right click on `src/test/java` folder and select Run 'All Tests'.
