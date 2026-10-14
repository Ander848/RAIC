package com.checkautos.repository.local;

import com.checkautos.models.User;
import com.checkautos.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@Profile("local")
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> store = new LinkedHashMap<>();

    public InMemoryUserRepository() {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        save(new User("Ander",   "ander@checkautos.com",   encoder.encode("1234")));
        save(new User("William", "william@checkautos.com", encoder.encode("1234")));
        save(new User("Daniela", "daniela@checkautos.com", encoder.encode("1234")));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return store.values().stream()
                .filter(u -> email.equals(u.getEmail()))
                .findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        return store.values().stream().anyMatch(u -> email.equals(u.getEmail()));
    }

    @Override public <S extends User> S save(S u) {
        if (u.getId() == null) u.setId(UUID.randomUUID().toString());
        store.put(u.getId(), u);
        return u;
    }
    @Override public List<User> findAll() { return new ArrayList<>(store.values()); }
    @Override public Optional<User> findById(String id) { return Optional.ofNullable(store.get(id)); }
    @Override public boolean existsById(String id) { return store.containsKey(id); }
    @Override public long count() { return store.size(); }
    @Override public void deleteById(String id) { store.remove(id); }
    @Override public void delete(User u) { store.remove(u.getId()); }
    @Override public void deleteAll() { store.clear(); }
    @Override public <S extends User> List<S> saveAll(Iterable<S> i) { i.forEach(this::save); return (List<S>) findAll(); }
    @Override public List<User> findAllById(Iterable<String> ids) { List<User> r = new ArrayList<>(); ids.forEach(id -> findById(id).ifPresent(r::add)); return r; }
    @Override public void deleteAll(Iterable<? extends User> i) { i.forEach(this::delete); }
    @Override public void deleteAllById(Iterable<? extends String> ids) { ids.forEach(this::deleteById); }
    @Override public List<User> findAll(Sort sort) { return findAll(); }
    @Override public Page<User> findAll(Pageable p) { return Page.empty(); }
    @Override public <S extends User> Optional<S> findOne(Example<S> e) { return Optional.empty(); }
    @Override public <S extends User> List<S> findAll(Example<S> e) { return Collections.emptyList(); }
    @Override public <S extends User> List<S> findAll(Example<S> e, Sort s) { return Collections.emptyList(); }
    @Override public <S extends User> Page<S> findAll(Example<S> e, Pageable p) { return Page.empty(); }
    @Override public <S extends User> long count(Example<S> e) { return 0; }
    @Override public <S extends User> boolean exists(Example<S> e) { return false; }
    @Override public <S extends User, R> R findBy(Example<S> e, Function<FluentQuery.FetchableFluentQuery<S>, R> f) { return null; }
    @Override public <S extends User> S insert(S s) { return save(s); }
    @Override public <S extends User> List<S> insert(Iterable<S> i) { return (List<S>) saveAll(i); }
}
