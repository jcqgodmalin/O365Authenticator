# O365Authenticator
This java class let's you connect to outlook using POP3 protocol with OAuth2.0

# Sample Usage:
```

//Instantiate the authenticator class
O365Authenticator auth = new O365Authenticator( tenantID, clientID, clientSecret );

//retrieve token
String token = auth.getToken();

//Instantiate the POP3SClient
POP3SClient client = auth.getClient( port );

try {
  client.connect( host );
  if (client.login( mailbox, token ) {
    try {
      //do your stuffs here. your app is already connected to your mailbox
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      try {
        client.logout();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
  } else {
      System.out.println( "Error logging in!");
  }
} catch (Exception ex) {
  ex.printStackTrace();
} finally {
  try {
    client.disconnect();
  } catch (Exception ex) {
    ex.printStackTrace();
  }
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

# Your app needs to be registered in Azure AD
Follow the steps in this guide to register your app in your Azure AD tenant.
https://learn.microsoft.com/en-us/azure/active-directory/develop/quickstart-register-app
Also, since this will access the O365 mailbox as an app, add the `POP.AccessAsApp` scope.
To do so, add a new scope -> API my organization uses -> Office365 Exchange Online -> POP -> POP.AccessAsApp

# Granting your app full access to the mailbox
Follow to the steps in this guide to grant your application full access to the O365 Mailbox. Please note that this needs to be performed by someone that has Global Administrator role in your Azure AD tenant.
https://github.com/jcqgodmalin/grant-o365-access-with-powershell
