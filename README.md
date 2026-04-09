**Steps to run backend server**
1. Change env.txt to .env and fill the required credentials

2. Run docker compose up --build to start the backend application

Server_Base_Url : http://localhost:8081/

Google Oauth Signin/Login : http://localhost:8081/oauth2/authorization/google         
returns ApiResponse<OauthLoginResponse> or ApiResponse<ErrorResponse>

Facebook Oauth Signin/Login : http://localhost:8081/oauth2/authorization/facebook   
return ApiResponse<OauthLoginResponse> or ApiResponse<ErrorResponse>
