package com.minibank.repo;

import com.minibank.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    boolean existsByEmail(String email);

    @Query("select c from Client c left join fetch c.accounts where c.id = :id")
    Optional<Client> findClientWithAccounts(Long id);

    @Query("select distinct c from Client c left join fetch c.accounts")
    List<Optional<Client>> findAllClientsWithAccounts();

    @Query("SELECT c FROM Client c ORDER BY c.id")
    List<Optional<Client>> findAllClients();
}
