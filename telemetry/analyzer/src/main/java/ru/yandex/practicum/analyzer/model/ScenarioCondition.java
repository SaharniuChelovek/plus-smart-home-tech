package ru.yandex.practicum.analyzer.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scenario_conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioCondition {

    @EmbeddedId
    private ScenarioConditionId id;

    @ManyToOne
    @MapsId("scenarioId")
    private Scenario scenario;

    @ManyToOne
    @MapsId("sensorId")
    private Sensor sensor;

    @ManyToOne
    @MapsId("conditionId")
    private Condition condition;
}