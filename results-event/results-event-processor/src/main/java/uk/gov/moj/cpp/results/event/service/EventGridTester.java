package uk.gov.moj.cpp.results.event.service;


import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;

public class EventGridTester {

    public static void main(String[] args) {
        EventGridPublisherClient<EventGridEvent> publisherClient = new EventGridPublisherClientBuilder()
                .endpoint("https://eg-ste-ccp0102-hearingres.uksouth-1.eventgrid.azure.net/api/events")  // make sure it accepts EventGridEvent
                .credential(new AzureKeyCredential("ljl9eirGvEQPEfCiio3HeB9E0RKn4gkMLdgOhVgBG0oChJTnFjT6JQQJ99AIACmepeSXJ3w3AAABAZEGCNsX"))
                .buildEventGridEventPublisherClient();

        // Create a EventGridEvent with String data
        String str = "FirstName: John1, LastName: James";
        EventGridEvent eventJson = new EventGridEvent("com/example/MyApp", "User.Created.Text", BinaryData.fromObject(str), "0.1");
        publisherClient.sendEvent(eventJson);
    }
}
