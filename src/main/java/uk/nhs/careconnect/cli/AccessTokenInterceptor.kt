package uk.nhs.careconnect.cli

import ca.uhn.fhir.rest.client.api.IClientInterceptor
import ca.uhn.fhir.rest.client.api.IHttpRequest
import ca.uhn.fhir.rest.client.api.IHttpResponse
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AccessToken
;
class AccessTokenInterceptor(authorizedClientService: OAuth2AuthorizedClientService, val clientId: String, val clientSecret : String, val tokenUrl :String, val apiKey : String) : IClientInterceptor {

    val clientRegistration =  ClientRegistration.withRegistrationId("aws")
            .clientId(clientId)
            .clientSecret(clientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.POST)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri(tokenUrl)
            .build()

    val inMemoryClientRegistrationRepository= InMemoryClientRegistrationRepository(clientRegistration)

    val authorizedClientManager: OAuth2AuthorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(inMemoryClientRegistrationRepository, authorizedClientService);

    override fun interceptRequest(request: IHttpRequest?) {
        val accessToken = getAccessToken()
        request?.addHeader("Authorization", "Bearer ${accessToken.tokenValue}")
        request?.addHeader("x-api-key", apiKey)
    }

    private fun getAccessToken(): OAuth2AccessToken {
        val authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("aws")
            .principal("test")
            .build()
        val authorizedClient = authorizedClientManager.authorize(authorizeRequest)
            ?: throw Error("Authorization failed")
        return authorizedClient.accessToken
    }

    override fun interceptResponse(theResponse: IHttpResponse?) {/*No change to response needed*/}
}
