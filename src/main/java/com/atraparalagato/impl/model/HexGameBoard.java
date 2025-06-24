package com.atraparalagato.impl.model;

import com.atraparalagato.base.model.GameBoard;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implementación de GameBoard para tableros hexagonales.
 */
public class HexGameBoard extends GameBoard<HexPosition> {

    private final Set<HexPosition> validPositions;
    private final int mySize;

    public HexGameBoard(int size) {
        super(size);
        this.mySize = size;
        this.validPositions = new HashSet<>(getAllPossiblePositions());
    }

    // Usamos HashSet por eficiencia y para evitar duplicados
    @Override
    protected Set<HexPosition> initializeBlockedPositions() {
        return new HashSet<>();
    }

    // VER SI ESTÁ DENTRO DE LOS LÍMITES CON EL SET PRECALCULADO
    @Override
    public boolean isPositionInBounds(HexPosition position) {
        return validPositions.contains(position);
    }

    // Es válido si está dentro de los límites y no está bloqueado
    @Override
    protected boolean isValidMove(HexPosition position) {
        return isPositionInBounds(position) && !isBlocked(position);
    }

    // EJECUTA MOVIMIENTO BLOQUEANDO LA POSICIÓN
    @Override
    public void executeMove(HexPosition position) {
        blockPosition(position);
        super.onMoveExecuted(position); // Esto incrementa el contador de la base
    }

    //LISTA DE POSICIONES QUE CUMPLEN LA CONDICIÓN DADA
    @Override
    public List<HexPosition> getPositionsWhere(Predicate<HexPosition> condition) {
        return validPositions.stream()
                .filter(condition)
                .collect(Collectors.toList());
    }

    //DEVUELVE LAS POSICIONES ADYACENTES DENTRO DEL TABLERO QUE NO ESTÁN BLOQUEADAS
    @Override
    public List<HexPosition> getAdjacentPositions(HexPosition position) {
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
                .filter(pos -> !isBlocked(pos))
                .collect(Collectors.toList());
    }

    //VERIFICA SI UNA POSICIÓN ESTÁ BLOQUEADA
    @Override
    public boolean isBlocked(HexPosition position) {
        return blockedPositions.contains(position);
    }

    //GENERA TODAS LAS POSICIONES POSIBLES DEL TABLERO
    private List<HexPosition> getAllPossiblePositions() {
        List<HexPosition> positions = new ArrayList<>();
        for (int q = -mySize; q <= mySize; q++) {
            for (int r = -mySize; r <= mySize; r++) {
                int s = -q - r;
                HexPosition pos = new HexPosition(q, r);
                if (Math.abs(q) <= mySize && Math.abs(r) <= mySize && Math.abs(s) <= mySize) {
                    positions.add(pos);
                }
            }
        }
        return positions;
    }

    @Override
    protected void onMoveExecuted(HexPosition position) {
        super.onMoveExecuted(position);
    }

    // === Métodos auxiliares para compatibilidad con HexGameService ===

    // Devuelve una copia de las posiciones bloqueadas (no sobrescribe ningún método final)
   
    public Set<HexPosition> getBlockedHexPositions() {
        return new HashSet<>(blockedPositions);
    }

    public void blockPosition(HexPosition pos) {
        blockedPositions.add(pos);
    }

    public List<HexPosition> getAllAvailablePositions() {
        return validPositions.stream().filter(pos -> !isBlocked(pos)).collect(Collectors.toList());
    }

    public List<HexPosition> getAllBorderPositions() {
        List<HexPosition> borders = new ArrayList<>();
        for (HexPosition pos : validPositions) {
            int s = pos.getS();
            if (Math.abs(pos.getQ()) == mySize || Math.abs(pos.getR()) == mySize || Math.abs(s) == mySize) {
                borders.add(pos);
            }
        }
        return borders;
    }
}