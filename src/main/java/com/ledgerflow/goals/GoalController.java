package com.ledgerflow.goals;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import static com.ledgerflow.goals.GoalDtos.*;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {
    private final GoalService goals;
    GoalController(GoalService goals) { this.goals = goals; }
    @GetMapping
    public List<GoalView> list(@AuthenticationPrincipal Jwt jwt) { return goals.list(UUID.fromString(jwt.getSubject())); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public GoalView create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateGoal request) { return goals.create(UUID.fromString(jwt.getSubject()), request); }
    @PutMapping("/{id}")
    public GoalView update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody UpdateGoal request) { return goals.update(UUID.fromString(jwt.getSubject()), id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestParam long version) { goals.delete(UUID.fromString(jwt.getSubject()), id, version); }
}
