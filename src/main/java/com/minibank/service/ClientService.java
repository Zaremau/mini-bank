package com.minibank.service;

import com.minibank.db.JpaUtil;
import com.minibank.dto.AccountResponse;
import com.minibank.dto.ClientResponse;
import com.minibank.dto.ClientWithAccountsResponse;
import com.minibank.exception.ClientNotFoundException;
import com.minibank.model.Client;
import com.minibank.repo.ClientRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Transactional
    public ClientResponse registerClient(String name, String email) {
        if(clientRepository.existsByEmail(email)){
            throw new IllegalStateException("Клиент с email " + email + " уже существует");
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
        try(EntityManager em = JpaUtil.getEntityManager()){
            List<Client> clients = em.createQuery(
                            "select distinct c from Client c left join fetch c.accounts",
                            Client.class)
                    .getResultList();

            return clients.stream()
                    .map(ClientWithAccountsResponse::from).toList();
        }
    }
}
