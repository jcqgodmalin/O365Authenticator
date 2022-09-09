# O365Authenticator
This java class let's you connect to outlook using POP3 protocol with OAuth2.0

# Sample Usage:
```

//Instantiate the authenticator class
O365Authenticator auth = new O365Authenticator( tenantID, clientID, clientSecret );

//retrieve token
String token = auth.getToken();

//Instantiate the POP3SClient
POP3SClient client = auth.getClient();

try {
  
  client.connect( host, port );
  
  if (client.login( mailbox, token ) {
  
    //do your stuffs here. your app is already connected to your mailbox
  
  } else {
    
      System.out.println( "Error logging in!");
  
  }

} catch (Exception ex) {
  
  ex.printStackTrace();
  
}


```

# Dependencies
This class uses the ff dependencies. Add them in your `POM.xml` file.

```

<!-- https://mvnrepository.com/artifact/org.apache.httpcomponents.client5/httpclient5 -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.1.3</version>
</dependency>

<dependency>
  <groupId>commons-net</groupId>
  <artifactId>commons-net</artifactId>
  <version>3.8.0</version>
</dependency>

```
