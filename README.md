Results Service



Event Grid publishing is enabled by default. Set the Boolean JNDI binding `java:global/eventGridPublishingEnabled` to `false` for an isolated environment without HEARINGRES/Event Grid. Disabled publishing skips SDK initialization and both hearing-result event sends. The existing `localhost` endpoint sentinel also skips SDK initialization. Enabled publishing retains endpoint/key validation and reports delivery failures as before.
