package airline.repository;

import airline.domain.user.User;

import java.util.Optional;

/** 회원 저장소. 지금은 메모리에 저장하지만 파일 저장 구현으로 바꿔도 서비스 코드는 그대로다. */
public interface UserRepository {

    void save(User user);

    Optional<User> findByUsername(String username);

    boolean exists(String username);
}
