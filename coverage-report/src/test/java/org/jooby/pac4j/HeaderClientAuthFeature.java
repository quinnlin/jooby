package org.jooby.pac4j;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.http.credentials.TokenCredentials;
import org.pac4j.http.credentials.authenticator.Authenticator;
import org.pac4j.http.profile.HttpProfile;

public class HeaderClientAuthFeature extends ServerFeature {

  public static class HeaderAuthenticator implements Authenticator<TokenCredentials> {

    @Override
    public void validate(final TokenCredentials credentials) {
      if (credentials == null || !credentials.getToken().equals("1234")) {
        throw new CredentialsException("Bad token");
      }
    }

  }

  {

    HeaderClient client = new HeaderClient();
    client.setHeaderName("X-Token");
    client.setAuthenticator(new HeaderAuthenticator());
    client.setProfileCreator(credentials -> {
      HttpProfile profile = new HttpProfile();
      profile.setId(credentials.getToken());
      return profile;
    });
    use(new Auth().client(client));

    get("/auth/header", req -> req.path());
  }

  @Test
  public void auth() throws Exception {
    request()
        .get("/auth/header")
        .header("X-Token", "1234")
        .expect("/auth/header");
  }

  @Test
  public void unauthorizedAjax() throws Exception {
    request()
        .get("/auth/header")
        .header("X-Requested-With", "XMLHttpRequest")
        .expect(401);
  }

  @Test
  public void unauthorized() throws Exception {
    request()
        .get("/auth/header")
        .expect(401);

  }

  @Test
  public void badCredentials() throws Exception {
    request()
        .get("/auth/header")
        .header("X-Token", "123")
        .expect(401);
  }

}
