package airline.repository;

import airline.domain.user.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** 메모리(Map)에 회원을 저장하는 구현. 프로그램을 끄면 사라진다. */
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> users = new HashMap<>();

    @Override
    public void save(User user) {
        users.put(user.getUsername(), user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    @Override
    public boolean exists(String username) {
        return users.containsKey(username);
    }
}
