package dev.study.kafkademo.repository;

import dev.study.kafkademo.entity.Engine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EngineRepository extends JpaRepository<Engine, Long> {

}
