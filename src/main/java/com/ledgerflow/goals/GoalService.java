package com.ledgerflow.goals;

import com.ledgerflow.shared.BusinessException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.ledgerflow.goals.GoalDtos.*;

@Service
@Transactional
public class GoalService {
    private final GoalRepository goals;
    GoalService(GoalRepository goals) { this.goals = goals; }
    @Transactional(readOnly = true)
    public List<GoalView> list(UUID owner) { return goals.findByOwnerIdOrderByCreatedAtDescIdDesc(owner).stream().map(GoalView::from).toList(); }
    public GoalView create(UUID owner, CreateGoal request) {
        return GoalView.from(goals.save(new Goal(owner, request.name(), request.targetAmount(), request.currentAmount(), request.dueDate())));
    }
    public GoalView update(UUID owner, UUID id, UpdateGoal request) {
        Goal goal = owned(owner, id);
        checkVersion(goal, request.version());
        goal.change(request.name(), request.targetAmount(), request.currentAmount(), request.dueDate());
        return GoalView.from(goals.saveAndFlush(goal));
    }
    public void delete(UUID owner, UUID id, long version) {
        Goal goal = owned(owner, id);
        checkVersion(goal, version);
        goals.delete(goal);
    }
    private Goal owned(UUID owner, UUID id) {
        return goals.lockOwned(id, owner).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "GOAL_NOT_FOUND", "Goal was not found."));
    }
    private void checkVersion(Goal goal, long version) {
        if (version < 0 || goal.getVersion() != version) throw new BusinessException(HttpStatus.CONFLICT, "GOAL_CHANGED", "This goal changed. Reload it before saving or deleting.");
    }
}
