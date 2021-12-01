package com.project.ipldashboard.data;


import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import com.project.ipldashboard.model.Team;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

    private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);

    private final EntityManager entityManager;

    @Autowired
    public JobCompletionNotificationListener(EntityManager entityManager) {
        this. entityManager = entityManager;
    }

    @Override
    @Transactional
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("!!! JOB FINISHED! Time to verify the results");
            Map<String,Team> teamData = new HashMap<>();
            entityManager.createQuery("select m.team1,count(*) from Match m group by m.team1",Object[].class) //it would return List<Object[]> results 
            .getResultList()
            .stream()
            .map(e-> new Team((String)e[0],(long)e[1]))
            .forEach(team-> teamData.put(team.getTeamName(), team));

            entityManager.createQuery("select m.team2,count(*) from Match m group by m.team2",Object[].class) //it would return List<Object[]> results 
           .getResultList()
           .stream()
           .forEach(e->{
               Team team = teamData.get(e[0]);
               team.setTotalMatches(team.getTotalMatches()+(long)e[1]);
           });

           entityManager.createQuery("select m.matchWinner , count(*) from Match m group by m.matchWinner",Object[].class)
           .getResultList()
           .stream()
           .forEach(e->{
            Team team = teamData.get(e[0]);
            if(team!=null)
            team.setTotalWins((long)e[1]);
           });

           teamData.values().forEach(team->entityManager.persist(team));


                






            // jdbcTemplate.query("SELECT team1, team2, date FROM match", 
            // (rs, row) -> " TEAM1 " + rs.getString(1)+" Team2 "+rs.getString(2)+" Date "+rs.getString(3))
            //         .forEach(str -> System.out.println(str));
        }
    }
}
