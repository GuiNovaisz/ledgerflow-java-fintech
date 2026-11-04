package com.ledgerflow.goals;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import com.ledgerflow.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.*;
import static com.ledgerflow.goals.GoalDtos.*;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {
    private final GoalService goals;
    GoalController(GoalService goals) { this.goals = goals; }
    @GetMapping
    public List<GoalView> list(@AuthenticatedUser UUID userId) { return goals.list(userId); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public GoalView create(@AuthenticatedUser UUID userId, @Valid @RequestBody CreateGoal request) { return goals.create(userId, request); }
    @PutMapping("/{id}")
    public GoalView update(@AuthenticatedUser UUID userId, @PathVariable UUID id, @Valid @RequestBody UpdateGoal request) { return goals.update(userId, id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticatedUser UUID userId, @PathVariable UUID id, @RequestParam long version) { goals.delete(userId, id, version); }
}
