**Steps to run spring boot application**
1. Open env.txt file , fill the required credentials and rename file env.txt to .env

2. Run docker compose up --build in terminal to start the backend application


#Bacend Server Configuration
Base_Url : http://localhost:8081

#Allowed Frontend Origin (CORS)
Base_Url : http://localhost:8080 or  http://localhost:3000

#Api Documentation Provided in 
Swagger Api docs url : http://localhost:8081/swagger-ui/index.html#

#open application.properties for signup confirmation url configuration


#Local Authentication Flow

1.User signup with email.
2.Confirmation link is send to user email inbox.
3.User clicks the link and email gets verified. 
4.User login with credentials.
5.Recieves login response including two jwt tokens (access and refresh token ) .
6.Store access token and refresh token in local storage .
7.Retrieve access token and add it in  Authorization header  i.e : Bearer `<jwtToken>`  for every request
8.Renew access token using refresh token since access token expires in every 30 minutes while refresh token expires in 7 days.

#Web OAuthentication Flow (Depreciated : introduced newer version App OAuthentication Flow)

1. User signup through provider (google/facebook).
2. First time oauth login gets user signup otherwise login.
3. Receives login response including jwt token.
4. Store jwt token in local storage.
5.  Retrieve jwt token and add it in  Authorization header  i.e : Bearer <jwtToken>  for every request

#App OAuthentication Flow (Applicable for both customer and admin login/signup)
1. Backend receives id_token , provider name , role.
2. First time login gets user signup : new admin accounts need approval from existing admin
3. Active account receives refresh and access token.

#Access Swagger Api docs url for better insights of api specifications.

#Web Oauth urls (older version : depreciated)
1. Url for Google Oauth Signup and Login : http://localhost:8081/oauth2/authorization/google         
-> returns ApiResponse<OauthLoginResponse> or ApiResponse<ErrorResponse>

2. Url for Facebook Oauth Signup and Login : http://localhost:8081/oauth2/authorization/facebook   
-> returns ApiResponse<OauthLoginResponse> or ApiResponse<ErrorResponse>
