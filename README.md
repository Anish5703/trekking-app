**Steps to run springboot application**
1. Open env.txt file , fill the required credentials and rename file env.txt to .env

2. Run docker compose up --build in terminal to start the backend application


#Bacend Server Configuration
Base_Url : http://localhost:8081

#Allowed Frontend Origin (CORS)
Base_Url : http://localhost:8080 or  http://localhost:3000

#Api Documentaion Provided in 
Swagger Api docs url : http://localhost:8081/swagger-ui/index.html#

#Local Authentication Flow

1.User signup with email.
2.Confirmation link is send to user email inbox.
3.User clicks the link and email gets verified.
4.User login with credentials.
5.Recieves login response including jwt token .
6.Store jwt token in local storage.
7.Retrieve jwt token and add it in  Authorization header  i.e : Bearer /<jwtToken/>  for every request

#OAuthentication Flow 

1. User signup through provider (google/facebook).
2. First time oauth login gets user signup otherwise login.
3. Recieves login response including jwt token.
4. Store jwt token in local storage.
5.  Retrieve jwt token and add it in  Authorization header  i.e : Bearer <jwtToken>  for every request


#Access Swagger Api docs url for better insights of api specifications.

#Oauth urls
1. Url for Google Oauth Signup and Login : http://localhost:8081/oauth2/authorization/google         
-> returns ApiResponse<OauthLoginResponse> or ApiResponse<ErrorResponse>

2. Url for Facebook Oauth Signup and Login : http://localhost:8081/oauth2/authorization/facebook   
-> returns ApiResponse<OauthLoginResponse> or ApiResponse<ErrorResponse>
