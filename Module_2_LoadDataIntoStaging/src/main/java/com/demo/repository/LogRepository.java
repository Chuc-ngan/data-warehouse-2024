package com.demo.repository;

import com.demo.entities.Config;
import com.demo.entities.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<Log, String>  {

    @Query("SELECT l FROM Log l WHERE (l.status = 'SUCCESS_LOAD_DATA' OR l.status = 'FAILURE_LOAD_DATA') " +
            "AND FUNCTION('DATE', l.createTime) = CURRENT_DATE")
    List<Log> findByStatusAndCreatedTimeToday();

}
