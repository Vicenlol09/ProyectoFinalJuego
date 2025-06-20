package com.atraparalagato.impl.model;

import com.atraparalagato.base.model.GameBoard;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implementación de GameBoard para tableros hexagonales.
 */
public class HexGameBoard extends GameBoard<HexPosition> {

    public HexGameBoard(int size) {
        super(size);
    }

    @Override
    protected Set<HexPosition> initializeBlockedPositions() {
        // Usamos HashSet por eficiencia y para evitar duplicados
        return new HashSet<>();
    }

    @Override
    protected boolean isPositionInBounds(HexPosition position) {
        // Valida que la posición esté dentro de los límites hexagonales
        int q = position.getQ();
        int r = position.getR();
        int s = position.getS();
        return Math.abs(q) <= size && Math.abs(r) <= size && Math.abs(s) <= size;
    }

    @Override
    protected boolean isValidMove(HexPosition position) {
        // Es válido si está dentro de los límites y no está bloqueado
        return isPositionInBounds(position) && !isBlocked(position);
    }

    @Override
    protected void executeMove(HexPosition position) {
        // Agrega la posición a las bloqueadas si es válida
        if (isValidMove(position)) {
            blockedPositions.add(position);
            onMoveExecuted(position);
        }
    }

    @Override
    public List<HexPosition> getPositionsWhere(Predicate<HexPosition> condition) {
        // Genera todas las posiciones posibles y filtra por el Predicate
        return getAllPossiblePositions().stream()
                .filter(condition)
                .collect(Collectors.toList());
    }

    @Override
    public List<HexPosition> getAdjacentPositions(HexPosition position) {
        // Direcciones hexagonales
        HexPosition[] directions = {
            new HexPosition(1, 0),
            new HexPosition(1, -1),
            new HexPosition(0, -1),
            new HexPosition(-1, 0),
            new HexPosition(-1, 1),
            new HexPosition(0, 1)
        };
        return Arrays.stream(directions)
                .map(dir -> (HexPosition) position.add(dir))
                .filter(this::isPositionInBounds)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isBlocked(HexPosition position) {
        // Consulta simple en el conjunto de bloqueadas
        return blockedPositions.contains(position);
    }

    // Método auxiliar para generar todas las posiciones válidas del tablero
    private List<HexPosition> getAllPossiblePositions() {
        List<HexPosition> positions = new ArrayList<>();
        for (int q = -size; q <= size; q++) {
            for (int r = -size; r <= size; r++) {
                int s = -q - r;
                HexPosition pos = new HexPosition(q, r);
                if (isPositionInBounds(pos)) {
                    positions.add(pos);
                }
            }
        }
        return positions;
    }

    @Override
    protected void onMoveExecuted(HexPosition position) {
        // Hook para lógica adicional tras un movimiento
        // Ejemplo: System.out.println("Movimiento ejecutado en: " + position);
        super.onMoveExecuted(position);
    }

    public Set<HexPosition> getBlockedPositions() {
        return new HashSet<>(blockedPositions);
    }
}