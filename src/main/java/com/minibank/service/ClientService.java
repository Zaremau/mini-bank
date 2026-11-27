package com.minibank.service;

import com.minibank.dto.ClientResponse;
import com.minibank.dto.ClientWithAccountsResponse;
import com.minibank.exception.AccountNotFoundException;
import com.minibank.exception.ClientNotFoundException;
import com.minibank.model.Client;
import com.minibank.repo.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.\\w+$");

    @Transactional
    public ClientResponse registerClient(String name, String email) {
        if(clientRepository.existsByEmail(email)){
            throw new IllegalStateException("Клиент с email " + email + " уже существует");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Некорректный формат email");
        }

        Client client = new Client(null, name, email);
        clientRepository.save(client);
        return ClientResponse.from(client);
    }

    public ClientResponse getClient(Long clientId) {
       Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Клиент с id=" + clientId + " не найден"));
       return ClientResponse.from(client);
    }

    public ClientWithAccountsResponse getClientWithAccounts(Long id) {
        Client client = clientRepository.findClientWithAccounts(id)
                .orElseThrow(() ->  new ClientNotFoundException("Клиент не найден"));
        return ClientWithAccountsResponse.from(client);
    }

    public List<ClientWithAccountsResponse> getAllClientsWithAccounts(){
            return clientRepository.findAllClientsWithAccounts()
                    .stream()
                    .map(cl ->
                            cl.orElseThrow(() -> new ClientNotFoundException("Клиент не найден")))
                    .map(ClientWithAccountsResponse::from).toList();
    }

    public List<ClientResponse> getAllClients() {
       return clientRepository.findAllClients().stream()
               .map(cl -> cl.orElseThrow(() -> new ClientNotFoundException("Клиент не найден")))
               .map(ClientResponse::from)
               .toList();
    }


    public void removeAccountFromClient(Long clientId, Long accountId) {
        Client client = clientRepository.findClientWithAccounts(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Клиент с id=" + clientId + " не найден"));

        boolean removed = client.getAccounts().removeIf(account -> accountId.equals(account.getId()));
        if (!removed) {
            throw new AccountNotFoundException("Счёт с ID " + accountId + " не найден");
        }
    }
}
