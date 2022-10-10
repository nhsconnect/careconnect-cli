package uk.nhs.careconnect.cli

import ca.uhn.fhir.rest.client.api.IClientInterceptor
import ca.uhn.fhir.rest.client.api.IHttpRequest
import ca.uhn.fhir.rest.client.api.IHttpResponse
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.core.OAuth2AccessToken;

class AccessTokenInterceptor(val authorizedClientManager: OAuth2AuthorizedClientManager ,  val apiKey : String) : IClientInterceptor {



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
