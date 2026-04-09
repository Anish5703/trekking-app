**Steps to run springboot application**
1. Open env.txt file , fill the required credentials and rename file env.txt to .env

2. Run docker compose up --build in terminal to start the backend application

Server_Base_Url : http://localhost:8081

Swagger Api docs url : http://localhost:8081/swagger-ui/index.html#

Url for Google Oauth Signup and Login : http://localhost:8081/oauth2/authorization/google         
-> returns ApiResponse<OauthLoginResponse> or ApiResponse<ErrorResponse>

Url for Facebook Oauth Signup and Login : http://localhost:8081/oauth2/authorization/facebook   
-> returns ApiResponse<OauthLoginResponse> or ApiResponse<ErrorResponse>
