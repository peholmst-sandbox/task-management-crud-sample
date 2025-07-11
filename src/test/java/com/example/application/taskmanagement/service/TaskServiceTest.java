package com.example.application.taskmanagement.service;

import com.example.application.TestcontainersConfiguration;
import com.example.application.security.CurrentUser;
import com.example.application.security.dev.SampleUsers;
import com.example.application.taskmanagement.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class TaskServiceTest {

    @Autowired
    TaskService taskService;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    CurrentUser currentUser;

    @Autowired
    TaskRepository taskRepository;

    @WithUserDetails(SampleUsers.USER_USERNAME)
    @Test
    void users_can_save_tasks() {
        var project = projectRepository.save(new Project("Test Project"));
        var task = new Task(project, ZoneId.systemDefault());
        taskService.saveTask(task);
        assertThat(taskService.hasTasks(project)).isTrue();
    }

    @WithUserDetails(SampleUsers.USER_USERNAME)
    @Test
    void users_cannot_delete_tasks() {
        var project = projectRepository.save(new Project("Test Project"));
        var task = taskService.saveTask(new Task(project, ZoneId.systemDefault()));
        assertThatThrownBy(() -> taskService.deleteTask(task)).isInstanceOf(AccessDeniedException.class);
        assertThat(taskService.hasTasks(project)).isTrue();
    }

    @WithUserDetails(SampleUsers.ADMIN_USERNAME)
    @Test
    void administrators_can_delete_tasks() {
        var project = projectRepository.save(new Project("Test Project"));
        var task = taskService.saveTask(new Task(project, ZoneId.systemDefault()));
        taskService.deleteTask(task);
        assertThat(taskService.hasTasks(project)).isFalse();
    }

    @WithUserDetails(SampleUsers.USER_USERNAME)
    @Test
    void create_task_uses_current_users_time_zone() {
        var project = projectRepository.save(new Project("Test Project"));
        var task = taskService.createTask(project);
        assertThat(task.getTimeZone()).isEqualTo(currentUser.require().getZoneId());
    }

    private void saveTestTasks(Project project) {
        taskRepository
                .save(new Task(project, ZoneId.systemDefault(), "First task", TaskStatus.PENDING, TaskPriority.LOW));
        taskRepository.save(
                new Task(project, ZoneId.systemDefault(), "Second task", TaskStatus.IN_PROGRESS, TaskPriority.NORMAL));
        taskRepository
                .save(new Task(project, ZoneId.systemDefault(), "Third task", TaskStatus.PAUSED, TaskPriority.HIGH));
        taskRepository
                .save(new Task(project, ZoneId.systemDefault(), "Fourth todo", TaskStatus.DONE, TaskPriority.URGENT));
    }

    @WithUserDetails(SampleUsers.USER_USERNAME)
    @Test
    void returns_all_tasks_for_empty_filter() {
        var project = projectRepository.save(new Project("Test Project"));
        saveTestTasks(project);
        assertThat(taskService.findTasks(project, null, PageRequest.ofSize(10))).hasSize(4);
        assertThat(taskService.findTasks(project, new TaskFilter(), PageRequest.ofSize(10))).hasSize(4);
    }

    @WithUserDetails(SampleUsers.USER_USERNAME)
    @Test
    void returns_matching_tasks_for_non_empty_filter() {
        var project = projectRepository.save(new Project("Test Project"));
        saveTestTasks(project);

        // Search term
        {
            var filter = new TaskFilter();
            filter.setSearchTerm("task");
            assertThat(taskService.findTasks(project, filter, PageRequest.ofSize(10))).hasSize(3);
        }

        // Status
        {
            var filter = new TaskFilter();
            filter.include(TaskStatus.PENDING);
            filter.include(TaskStatus.PAUSED);
            assertThat(taskService.findTasks(project, filter, PageRequest.ofSize(10))).hasSize(2);
        }

        // Priority
        {
            var filter = new TaskFilter();
            filter.include(TaskPriority.HIGH);
            filter.include(TaskPriority.URGENT);
            assertThat(taskService.findTasks(project, filter, PageRequest.ofSize(10))).hasSize(2);
        }
    }
}
