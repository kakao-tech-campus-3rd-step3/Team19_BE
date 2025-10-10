package com.team19.musuimsa.user.repository;

import com.team19.musuimsa.user.domain.UserDevice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    List<UserDevice> findByUser_UserId(Long userId);

}
