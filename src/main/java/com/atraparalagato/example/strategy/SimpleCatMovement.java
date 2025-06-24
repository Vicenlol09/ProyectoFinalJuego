package com.atraparalagato.example.strategy;

import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.base.strategy.CatMovementStrategy;
import com.atraparalagato.impl.model.HexGameBoard;
import com.atraparalagato.impl.model.HexPosition;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Estrategia de movimiento simple para el gato.
 * 
 * NOTA PARA ESTUDIANTES:
 * Esta es una implementación BÁSICA e INTENCIONALMENTE POCO INTELIGENTE.
 * Los estudiantes deben:
 * 1. Estudiar esta implementación para entender los conceptos básicos
 * 2. Crear estrategias más sofisticadas (A*, BFS, DFS, etc.)
 * 3. Implementar heurísticas más inteligentes
 * 
 * Esta estrategia usa lógica muy simple:
 * - Movimiento aleatorio con preferencia hacia el borde
 * - Sin pathfinding avanzado
 * - Heurística básica de distancia
 */
public class SimpleCatMovement extends CatMovementStrategy<HexPosition> {
    
    private final Random random = new Random();
    
    
public Optional<HexPosition> getNextMove(HexGameBoard board, HexPosition catPosition) {
    // Busca los posibles movimientos desde la posición actual
    List<HexPosition> possibleMoves = board.getAdjacentPositions(catPosition).stream()
            .filter(pos -> !board.isBlocked(pos))
            .toList();

    // Usa selectBestMove para elegir el mejor movimiento
    return selectBestMove(possibleMoves, catPosition, null);
}

    public SimpleCatMovement(GameBoard<HexPosition> board) {
        super(board);
    }
    
    @Override
    protected List<HexPosition> getPossibleMoves(HexPosition currentPosition) {
        // Obtener posiciones adyacentes que no estén bloqueadas
        return board.getAdjacentPositions(currentPosition).stream()
                .filter(pos -> !board.isBlocked(pos))
                .toList();
    }
    
    @Override
    protected Optional<HexPosition> selectBestMove(List<HexPosition> possibleMoves, 
                                                  HexPosition currentPosition, 
                                                  HexPosition targetPosition) {
        if (possibleMoves.isEmpty()) {
            return Optional.empty();
        }
        
        // Estrategia simple: preferir movimientos hacia el borde
        // Pero con algo de aleatoriedad para no ser predecible
        
        // 70% de probabilidad de elegir el "mejor" movimiento
        // 30% de probabilidad de movimiento aleatorio
        if (random.nextDouble() < 0.7) {
            return selectBestMoveTowardsBorder(possibleMoves);
        } else {
            return selectRandomMove(possibleMoves);
        }
    }
    
    @Override
    protected Function<HexPosition, Double> getHeuristicFunction(HexPosition targetPosition) {
        // Heurística simple: distancia al borde más cercano
        return position -> {
            int boardSize = board.getSize();
            
            // Calcular distancia mínima al borde
            double distanceToBorder = Math.min(
                Math.min(boardSize - Math.abs(position.getQ()),
                        boardSize - Math.abs(position.getR())),
                boardSize - Math.abs(position.getS())
            );
            
            // Invertir para que menor distancia al borde = mejor puntuación
            return boardSize - distanceToBorder;
        };
    }
    
    @Override
    protected Predicate<HexPosition> getGoalPredicate() {
        // El objetivo es llegar al borde del tablero
        return position -> {
            int boardSize = board.getSize();
            return Math.abs(position.getQ()) >= boardSize ||
                   Math.abs(position.getR()) >= boardSize ||
                   Math.abs(position.getS()) >= boardSize;
        };
    }
    
    @Override
    protected double getMoveCost(HexPosition from, HexPosition to) {
        // Costo uniforme para movimientos adyacentes
        return 1.0;
    }
    
    @Override
    public boolean hasPathToGoal(HexPosition currentPosition) {
        // Implementación muy básica: BFS simple para verificar si hay camino al borde
        Set<HexPosition> visited = new HashSet<>();
        Queue<HexPosition> queue = new LinkedList<>();
        
        queue.offer(currentPosition);
        visited.add(currentPosition);
        
        while (!queue.isEmpty()) {
            HexPosition current = queue.poll();
            
            // Si llegamos al borde, hay camino
            if (getGoalPredicate().test(current)) {
                return true;
            }
            
            // Explorar vecinos no bloqueados
            for (HexPosition neighbor : board.getAdjacentPositions(current)) {
                if (!visited.contains(neighbor) && !board.isBlocked(neighbor)) {
                    visited.add(neighbor);
                    queue.offer(neighbor);
                }
            }
        }
        
        return false; // No se encontró camino al borde
    }
    
    @Override
    public List<HexPosition> getFullPath(HexPosition currentPosition, HexPosition targetPosition) {
        // Implementación básica de BFS para encontrar camino
        Map<HexPosition, HexPosition> parentMap = new HashMap<>();
        Set<HexPosition> visited = new HashSet<>();
        Queue<HexPosition> queue = new LinkedList<>();
        
        queue.offer(currentPosition);
        visited.add(currentPosition);
        parentMap.put(currentPosition, null);
        
        while (!queue.isEmpty()) {
            HexPosition current = queue.poll();
            
            // Si llegamos al objetivo o al borde, reconstruir camino
            if (current.equals(targetPosition) || getGoalPredicate().test(current)) {
                return reconstructPath(parentMap, current);
            }
            
            // Explorar vecinos
            for (HexPosition neighbor : board.getAdjacentPositions(current)) {
                if (!visited.contains(neighbor) && !board.isBlocked(neighbor)) {
                    visited.add(neighbor);
                    parentMap.put(neighbor, current);
                    queue.offer(neighbor);
                }
            }
        }
        
        return Collections.emptyList(); // No se encontró camino
    }
    
    // Métodos auxiliares privados
    
    private Optional<HexPosition> selectBestMoveTowardsBorder(List<HexPosition> possibleMoves) {
        Function<HexPosition, Double> heuristic = getHeuristicFunction(null);
        
        return possibleMoves.stream()
                .max(Comparator.comparing(heuristic::apply));
    }
    
    private Optional<HexPosition> selectRandomMove(List<HexPosition> possibleMoves) {
        if (possibleMoves.isEmpty()) {
            return Optional.empty();
        }
        
        int randomIndex = random.nextInt(possibleMoves.size());
        return Optional.of(possibleMoves.get(randomIndex));
    }
    
    private List<HexPosition> reconstructPath(Map<HexPosition, HexPosition> parentMap, 
                                            HexPosition goal) {
        List<HexPosition> path = new ArrayList<>();
        HexPosition current = goal;
        
        while (current != null) {
            path.add(current);
            current = parentMap.get(current);
        }
        
        Collections.reverse(path);
        return path;
    }
    
    // Hook methods con implementación básica
    @Override
    protected void beforeMovementCalculation(HexPosition currentPosition) {
        // Log básico para debugging
        System.out.println("Calculando movimiento del gato desde: " + currentPosition);
    }
    
    @Override
    protected void afterMovementCalculation(Optional<HexPosition> selectedMove) {
        if (selectedMove.isPresent()) {
            System.out.println("Gato se mueve a: " + selectedMove.get());
        } else {
            System.out.println("¡Gato no puede moverse! Está atrapado.");
        }
    }
} 