package com.minibank.httphandler;

import com.minibank.dto.ClientResponse;
import com.minibank.dto.ClientWithAccountsResponse;
import com.minibank.dto.RegisterClientRequest;
import com.minibank.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController()
@RequestMapping("/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @PostMapping()
    public ResponseEntity<Void> registerClient(@RequestBody RegisterClientRequest request) {
        ClientResponse client = clientService.registerClient(request.name(), request.email());
        return ResponseEntity.status(HttpStatus.CREATED).build();

    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClient(@PathVariable Long id) throws IOException{
       return ResponseEntity.ok(clientService.getClient(id));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<ClientWithAccountsResponse> getClientWithAccounts(@PathVariable Long id) throws IOException{
        ClientWithAccountsResponse client = clientService.getClientWithAccounts(id);
        return ResponseEntity.status(HttpStatusCode.valueOf(200)).body(client);
    }

    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAllClients() throws IOException {
        List<ClientResponse> clients = clientService.getAllClients();
        return ResponseEntity.status(HttpStatusCode.valueOf(200)).body(clients);
    }

    @GetMapping("/details")
    public ResponseEntity<List<ClientWithAccountsResponse>> getAllClientsWithAccounts() throws IOException {
        List<ClientWithAccountsResponse> clients = clientService.getAllClientsWithAccounts();
        return ResponseEntity.status(HttpStatusCode.valueOf(200)).body(clients);
    }

    @DeleteMapping("/{id}/accounts/{id}")
    private ResponseEntity<Void> removeAccountFromClient(@PathVariable Long clientId, @PathVariable Long accountId) throws IOException {
        clientService.removeAccountFromClient(clientId, accountId);
        return ResponseEntity.status(HttpStatusCode.valueOf(204)).build();
    }
}
