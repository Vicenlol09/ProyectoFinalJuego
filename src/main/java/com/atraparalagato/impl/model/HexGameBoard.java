package com.atraparalagato.impl.model;

import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.impl.model.HexPosition;


import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implementación de GameBoard para tableros hexagonales.
 */
public class HexGameBoard extends GameBoard<HexPosition> {

    // POSICIONES VÁLIDAS DEL TABLERO (CONSULTAS RÁPIDAS)
    private final Set<HexPosition> validPositions; 

    public HexGameBoard(int size) {
        super(size);
        this.validPositions = new HashSet<>(getAllPossiblePositions()); 
    }


    // Usamos HashSet por eficiencia y para evitar duplicados
    @Override
    protected Set<HexPosition> initializeBlockedPositions() {
        
        return new HashSet<>();
    }

    // VER SI ESTÁ DENTRO DE LOS LÍMITES CON EL SET PRECALCULADO
    @Override
    protected boolean isPositionInBounds(HexPosition position) { 
        return validPositions.contains(position);
    }

    // Es válido si está dentro de los límites y no está bloqueado
    @Override
    protected boolean isValidMove(HexPosition position) {
        return isPositionInBounds(position) && !isBlocked(position);
    }

    // EJECUTA MOVIMIENTO BLOQUEANDO LA POSICIÓN
    @Override
    protected void executeMove(HexPosition position) {
        if (!isValidMove(position)) {
            throw new IllegalArgumentException("Movimiento inválido: " + position);
        }
        blockedPositions.add(position);//AÑADE LA POSICIÓN A LAS BLOQUEADAS
        onMoveExecuted(position); // Llamada al hook para lógica adicional tras el movimiento
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
        // Consulta simple en el conjunto de bloqueadas
        return blockedPositions.contains(position);
    }


    //GENERA TODAS LAS POSICIONES POSIBLES DEL TABLERO
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

    @Override
    public Set<HexPosition> getBlockedPositionsview() {
        return Collections.unmodifiableSet(blockedPositions);
    }
}