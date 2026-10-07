package qa.api.service;

import qa.api.client.BankClient;

public record Fixture(
    BankClient client, int customerId, int accountId, String username, String password) {}
