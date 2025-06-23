package com.atraparalagato.impl.strategy;

import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.base.strategy.CatMovementStrategy;
import com.atraparalagato.impl.model.HexPosition;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class BFSCatMovement extends CatMovementStrategy<HexPosition> {

    public BFSCatMovement(GameBoard<HexPosition> board) {
        super(board);
    }

    @Override
    public List<HexPosition> getPossibleMoves(HexPosition currentPosition) {
        // Devuelve las posiciones adyacentes no bloqueadas
        return board.getAdjacentPositions(currentPosition).stream()
                .filter(pos -> !board.isBlocked(pos))
                .toList();
    }

    @Override
    public Optional<HexPosition> selectBestMove(List<HexPosition> possibleMoves,
                                                  HexPosition currentPosition,
                                                  HexPosition targetPosition) {
        if (possibleMoves.isEmpty()) return Optional.empty();

        Predicate<HexPosition> isGoal = getGoalPredicate();
        List<HexPosition> bestPath = null;

        for (HexPosition move : possibleMoves) {
            List<HexPosition> path = bfsToGoal(move).orElse(null);
            if (path != null && !path.isEmpty() && isGoal.test(path.get(path.size() - 1))) {
                if (bestPath == null || path.size() < bestPath.size()) {
                    bestPath = path;
                }
            }
        }

        if (bestPath != null && !bestPath.isEmpty()) {
            return Optional.of(bestPath.get(0));
        }
        return Optional.empty();
    }

    @Override
    protected Function<HexPosition, Double> getHeuristicFunction(HexPosition targetPosition) {
        // No es necesario para BFS, pero se puede usar la distancia hexagonal para desempate
        return position -> (double) position.distanceTo(targetPosition);
    }

    @Override
    protected Predicate<HexPosition> getGoalPredicate() {
        int size = board.getSize();
        // El objetivo es cualquier celda en el borde del tablero
        return pos -> Math.abs(pos.getQ()) == size ||
                      Math.abs(pos.getR()) == size ||
                      Math.abs(pos.getS()) == size;
    }

    @Override
    protected double getMoveCost(HexPosition from, HexPosition to) {
        // Costo uniforme para BFS
        return 1.0;
    }

    @Override
    public boolean hasPathToGoal(HexPosition currentPosition) {
        return bfsToGoal(currentPosition).isPresent();
    }

    @Override
    public List<HexPosition> getFullPath(HexPosition currentPosition, HexPosition targetPosition) {
        // BFS hasta targetPosition específico
        Queue<HexPosition> queue = new LinkedList<>();
        Map<HexPosition, HexPosition> parentMap = new HashMap<>();
        Set<HexPosition> visited = new HashSet<>();

        queue.add(currentPosition);
        visited.add(currentPosition);

        while (!queue.isEmpty()) {
            HexPosition pos = queue.poll();
            if (pos.equals(targetPosition)) {
                return reconstructPath(parentMap, currentPosition, targetPosition);
            }
            for (HexPosition neighbor : getPossibleMoves(pos)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parentMap.put(neighbor, pos);
                    queue.add(neighbor);
                }
            }
        }
        return Collections.emptyList();
    }

    // Ejecuta BFS desde una posición hasta cualquier objetivo (borde)
    private Optional<List<HexPosition>> bfsToGoal(HexPosition start) {
        Predicate<HexPosition> isGoal = getGoalPredicate();
        Queue<HexPosition> queue = new LinkedList<>();
        Map<HexPosition, HexPosition> parentMap = new HashMap<>();
        Set<HexPosition> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            HexPosition pos = queue.poll();
            if (isGoal.test(pos)) {
                return Optional.of(reconstructPath(parentMap, start, pos));
            }
            for (HexPosition neighbor : getPossibleMoves(pos)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parentMap.put(neighbor, pos);
                    queue.add(neighbor);
                }
            }
        }
        return Optional.empty();
    }

    // Reconstruye el camino desde el mapa de padres
    private List<HexPosition> reconstructPath(Map<HexPosition, HexPosition> parentMap,
                                              HexPosition start, HexPosition goal) {
        List<HexPosition> path = new LinkedList<>();
        HexPosition current = goal;
        while (current != null && !current.equals(start)) {
            path.add(0, current);
            current = parentMap.get(current);
        }
        if (current != null) {
            path.add(0, start);
        }
        return path;
    }
}