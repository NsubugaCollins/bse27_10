import random
import numpy as np
from typing import List, Dict, Any

class GeneticOptimizer:
    def __init__(self, steps: List[Dict[str, Any]]):
        self.steps = steps
        # Define constants for cost functions
        self.energy_cost_rate = 0.15      # $ per kWh
        self.downtime_cost_rate = 150.0   # $ per hour of repair downtime
        self.delay_penalty_rate = 50.0    # $ per hour of production time (makespan)
        self.target_item_count = 100

    def evaluate_chromosome(self, speeds: List[float]) -> float:
        """
        Evaluate a chromosome (list of speed multipliers, e.g. [1.2, 0.8, 1.5]).
        Returns total cost (fitness - to be minimized).
        """
        makespan_minutes = 0.0
        total_energy_kwh = 0.0
        total_downtime_minutes = 0.0

        # Simple queuing queue factor: if a step is slower than the previous one, it creates a queue
        prev_throughput = 9999.0

        for idx, step in enumerate(self.steps):
            speed_mult = speeds[idx]
            
            # Base machine properties
            base_duration = step.get("duration", 5.0)
            power = step.get("powerConsumption", 5.0)
            maint_interval = step.get("maintenanceInterval", 100.0)

            # Active processing time for this step
            step_duration = base_duration / speed_mult
            
            # Calculate resource throughput capacity
            step_throughput = speed_mult / base_duration

            # Estimated queuing delay: if throughput is lower than arrival or previous stage
            queue_delay = 0.0
            if step_throughput < prev_throughput:
                queue_delay = (1.0 / step_throughput - 1.0 / prev_throughput) * self.target_item_count * 0.4
                queue_delay = max(0.0, queue_delay)
            
            prev_throughput = step_throughput

            # Total processing + waiting time for all items
            step_total_time = (step_duration * self.target_item_count) + queue_delay
            makespan_minutes += step_total_time

            # Energy: active power scales non-linearly with speed (speed^1.5)
            adjusted_power = power * (speed_mult ** 1.5)
            step_energy = adjusted_power * (step_duration * self.target_item_count / 60.0)
            total_energy_kwh += step_energy

            # Breakdowns: failure probability scales quadratically with speed (speed^2)
            # Failure rate per hour
            failure_rate = (speed_mult ** 2.0) / maint_interval
            step_operating_hours = (step_duration * self.target_item_count) / 60.0
            expected_failures = failure_rate * step_operating_hours
            expected_repair_time_mins = expected_failures * 15.0 # 15 mins average repair
            total_downtime_minutes += expected_repair_time_mins

        makespan_hours = makespan_minutes / 60.0
        downtime_hours = total_downtime_minutes / 60.0

        # Calculate costs
        energy_cost = total_energy_kwh * self.energy_cost_rate
        downtime_cost = downtime_hours * self.downtime_cost_rate
        delay_cost = makespan_hours * self.delay_penalty_rate

        return energy_cost + downtime_cost + delay_cost

    def optimize(self, generations: int = 40, pop_size: int = 30) -> Dict[str, Any]:
        num_genes = len(self.steps)
        if num_genes == 0:
            return {}

        # Population: list of random speed multipliers between 0.6x and 1.8x
        population = [
            [random.uniform(0.6, 1.8) for _ in range(num_genes)]
            for _ in range(pop_size)
        ]

        # Keep track of baseline (1.0 speed multiplier for all genes)
        baseline_speeds = [1.0] * num_genes
        baseline_cost = self.evaluate_chromosome(baseline_speeds)

        best_chromosome = None
        best_cost = float('inf')

        # Genetic Algorithm loop
        for _ in range(generations):
            # Evaluate fitness
            scored_pop = [(self.evaluate_chromosome(ind), ind) for ind in population]
            scored_pop.sort(key=lambda x: x[0])

            # Track global best
            if scored_pop[0][0] < best_cost:
                best_cost = scored_pop[0][0]
                best_chromosome = scored_pop[0][1]

            # Selection (Elitism + tournament)
            new_pop = [scored_pop[0][1], scored_pop[1][1]] # Elitism: keep best 2

            while len(new_pop) < pop_size:
                # Tournament selection
                parent1 = self.tournament(scored_pop)
                parent2 = self.tournament(scored_pop)

                # Crossover (single-point)
                if random.random() < 0.8 and num_genes > 1:
                    cross_point = random.randint(1, num_genes - 1)
                    child1 = parent1[:cross_point] + parent2[cross_point:]
                    child2 = parent2[:cross_point] + parent1[cross_point:]
                else:
                    child1, child2 = parent1.copy(), parent2.copy()

                # Mutation
                self.mutate(child1)
                self.mutate(child2)

                new_pop.extend([child1, child2])

            population = new_pop[:pop_size]

        # Calculate final metrics for the best configuration
        opt_speeds = best_chromosome
        
        # Format results
        recommendations = []
        for idx, step in enumerate(self.steps):
            rec_speed = round(opt_speeds[idx] * 100.0, 0)
            recommendations.append({
                "stepOrder": step.get("stepOrder"),
                "name": step.get("name"),
                "resourceName": step.get("resourceName", "Resource"),
                "resourceType": step.get("resourceType"),
                "resourceId": step.get("resourceId"),
                "recommendedSpeedPercent": rec_speed,
                "action": "Increase speed" if rec_speed > 105 else ("Decrease speed (save energy/wear)" if rec_speed < 95 else "Maintain current speed")
            })

        cost_saving_pct = round(((baseline_cost - best_cost) / baseline_cost * 100.0), 1) if baseline_cost > 0 else 0.0

        return {
            "baselineCostUSD": round(baseline_cost, 2),
            "optimizedCostUSD": round(best_cost, 2),
            "costSavingPercent": max(0.0, cost_saving_pct),
            "recommendations": recommendations
        }

    def tournament(self, scored_pop: List[Any], k: int = 3) -> List[float]:
        candidates = random.sample(scored_pop, k)
        candidates.sort(key=lambda x: x[0])
        return candidates[0][1]

    def mutate(self, chromosome: List[float]):
        for i in range(len(chromosome)):
            if random.random() < 0.15: # 15% mutation rate
                # Mutate by adding small random value, keep within bounds
                chromosome[i] = max(0.5, min(2.0, chromosome[i] + random.normalvariate(0.0, 0.2)))


class RLScheduler:
    """
    Reinforcement Learning Q-Learning Scheduler Agent for Autonomous Shop Floor Dispatching.
    Learns optimal dynamic dispatching policies under stochastic machine degradation and breakdowns.
    """
    def __init__(self, steps: List[Dict[str, Any]], item_count: int = 100):
        self.steps = steps
        self.item_count = item_count
        self.actions = ["SPT (Shortest Processing Time)", "LPT (Longest Processing Time)", "HAD (Health-Aware Dispatch)", "EMD (Energy-Minimizing Dispatch)"]
        self.q_table: Dict[str, List[float]] = {}
        self.alpha = 0.1  # Learning rate
        self.gamma = 0.9  # Discount factor
        self.epsilon = 0.2 # Epsilon-greedy exploration

    def _get_state(self, queue_len: int, health_lvl: str, bottleneck_active: bool) -> str:
        return f"Q:{min(queue_len // 10, 5)}_H:{health_lvl}_B:{1 if bottleneck_active else 0}"

    def train(self, episodes: int = 60) -> Dict[str, Any]:
        rewards_history = []
        best_makespan = float('inf')

        for ep in range(episodes):
            total_reward = 0.0
            current_makespan = 0.0
            
            # Decay exploration rate over time
            eps = max(0.02, self.epsilon * (1.0 - ep / episodes))

            for idx, step in enumerate(self.steps):
                duration = step.get("duration", 5.0)
                maint_health = max(30, 100 - idx * 12)
                health_lvl = "HIGH" if maint_health > 80 else ("MED" if maint_health > 50 else "LOW")
                state = self._get_state(self.item_count // (idx + 1), health_lvl, idx == 0)

                if state not in self.q_table:
                    self.q_table[state] = [0.0] * len(self.actions)

                # Select action (epsilon-greedy)
                if random.random() < eps:
                    action_idx = random.randint(0, len(self.actions) - 1)
                else:
                    action_idx = int(np.argmax(self.q_table[state]))

                # Compute step execution under chosen action policy multiplier
                if action_idx == 0:    # SPT
                    policy_mult = 1.3
                elif action_idx == 1:  # LPT
                    policy_mult = 0.85
                elif action_idx == 2:  # HAD (Health-Aware)
                    policy_mult = 1.1 if maint_health > 70 else 0.8
                else:                  # EMD (Energy-Minimizing)
                    policy_mult = 0.95

                step_duration = (duration / policy_mult) * self.item_count / 60.0 # in hours
                current_makespan += step_duration

                # Reward: negative makespan + health penalty
                reward = - (step_duration * 10.0)
                if maint_health < 50 and policy_mult > 1.1:
                    reward -= 25.0  # Penalty for overstressing degrading machine

                total_reward += reward

                # Next state approximation & Q-table Bellman update
                next_state = self._get_state(self.item_count // (idx + 2), health_lvl, False)
                if next_state not in self.q_table:
                    self.q_table[next_state] = [0.0] * len(self.actions)

                max_future_q = max(self.q_table[next_state])
                self.q_table[state][action_idx] += self.alpha * (reward + self.gamma * max_future_q - self.q_table[state][action_idx])

            rewards_history.append(round(total_reward, 2))
            if current_makespan < best_makespan:
                best_makespan = current_makespan

        # Extract optimal policies
        optimal_policy = []
        for idx, step in enumerate(self.steps):
            health_lvl = "HIGH" if (100 - idx * 12) > 80 else "MED"
            state = self._get_state(self.item_count // (idx + 1), health_lvl, idx == 0)
            q_vals = self.q_table.get(state, [0.0] * len(self.actions))
            best_action_idx = int(np.argmax(q_vals))

            optimal_policy.append({
                "stepOrder": step.get("stepOrder", idx + 1),
                "stepName": step.get("name", f"Step #{idx+1}"),
                "resourceName": step.get("resourceName", "Machine"),
                "optimalDispatchPolicy": self.actions[best_action_idx],
                "qScore": round(float(q_vals[best_action_idx]), 3),
            })

        return {
            "trainedEpisodes": episodes,
            "convergenceHistory": rewards_history,
            "bestMakespanHours": round(best_makespan, 2),
            "optimalDispatchRules": optimal_policy,
            "qTableSize": len(self.q_table)
        }


class CarbonFootprintCalculator:
    """
    Calculates Scope 1 (Direct combustion) and Scope 2 (Indirect electricity) CO2 emissions.
    Provides carbon intensity scoring and actionable sustainability recommendations.
    """
    def __init__(self, energy_kwh: float, item_count: int = 100, grid_source: str = "grid_mix", fuel_liters: float = 0.0):
        self.energy_kwh = energy_kwh
        self.item_count = max(1, item_count)
        self.grid_source = grid_source
        self.fuel_liters = fuel_liters

        # Grid CO2 emission factors in kg CO2 / kWh
        self.EMISSION_FACTORS = {
            "coal": 0.820,
            "natural_gas": 0.380,
            "grid_mix": 0.475,
            "renewable": 0.020,
            "solar_wind": 0.015,
        }
        # Diesel/Fuel factor: 2.68 kg CO2 per liter
        self.FUEL_EMISSION_FACTOR = 2.68

    def calculate(self) -> Dict[str, Any]:
        grid_factor = self.EMISSION_FACTORS.get(self.grid_source.lower(), 0.475)
        
        # Scope 1 (Direct Fuel) & Scope 2 (Grid Electricity)
        scope1_co2_kg = self.fuel_liters * self.FUEL_EMISSION_FACTOR
        scope2_co2_kg = self.energy_kwh * grid_factor
        total_co2_kg = scope1_co2_kg + scope2_co2_kg
        co2_per_item_kg = total_co2_kg / self.item_count

        # Sustainability Rating
        if co2_per_item_kg < 0.2:
            rating = "A+ (Net Zero Ready)"
            color = "EMERALD"
        elif co2_per_item_kg < 0.5:
            rating = "A (Highly Efficient)"
            color = "EMERALD"
        elif co2_per_item_kg < 1.0:
            rating = "B (Moderate Impact)"
            color = "CYAN"
        elif co2_per_item_kg < 2.0:
            rating = "C (High Emissions)"
            color = "AMBER"
        else:
            rating = "D (Critical Carbon Intensity)"
            color = "ROSE"

        # Eco-optimization recommendations
        recommendations = []
        if self.grid_source in ["coal", "grid_mix"]:
            potential_saving = total_co2_kg * 0.6
            recommendations.append(f"Switch factory energy supplier to Solar/Wind grid mix to reduce emissions by ~{round(potential_saving, 1)} kg CO2.")
        if self.energy_kwh > 150:
            recommendations.append("Enable Variable Frequency Drives (VFD) on high-power press motors during idle gaps.")
        recommendations.append("Schedule heavy machining jobs during off-peak green energy windows.")

        return {
            "totalEnergyKWh": round(self.energy_kwh, 2),
            "scope1DirectCO2Kg": round(scope1_co2_kg, 2),
            "scope2IndirectCO2Kg": round(scope2_co2_kg, 2),
            "totalCO2EmissionsKg": round(total_co2_kg, 2),
            "co2PerItemKg": round(co2_per_item_kg, 3),
            "sustainabilityRating": rating,
            "ratingColor": color,
            "gridSource": self.grid_source,
            "ecoRecommendations": recommendations,
        }

