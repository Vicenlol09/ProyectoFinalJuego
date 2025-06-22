package com.atraparalagato.impl.model;

import com.atraparalagato.base.model.Position;

/**
 * Implementación concreta de Position para coordenadas hexagonales.
 */
public class HexPosition extends Position {

    private final int q; // Coordenada axial q
    private final int r; // Coordenada axial r

    public HexPosition(int q, int r) {
        this.q = q;
        this.r = r;
    }

    public int getQ() {
        return q;
    }

    public int getR() {
        return r;
    }

    public int getS() {
        return -q - r; // Tercera coordenada axial
    }

    @Override
    public double distanceTo(Position other) {
        if (!(other instanceof HexPosition hex)) {
            throw new IllegalArgumentException("Cannot calculate distance to non-hex position");
        }
        // Distancia hexagonal (axial)
        return (Math.abs(q - hex.q) + Math.abs(r - hex.r) + Math.abs(getS() - hex.getS())) / 2.0;
    }

    @Override
    public Position add(Position other) {
        if (!(other instanceof HexPosition hex)) {
            throw new IllegalArgumentException("Cannot add non-hex position");
        }
        return new HexPosition(q + hex.q, r + hex.r);
    }

    @Override
    public Position subtract(Position other) {
        if (!(other instanceof HexPosition hex)) {
            throw new IllegalArgumentException("Cannot subtract non-hex position");
        }
        return new HexPosition(q - hex.q, r - hex.r);
    }

    @Override
    public boolean isAdjacentTo(Position other) {
        return distanceTo(other) == 1.0;
    }

    @Override
    public boolean isWithinBounds(int maxSize) {
        return Math.abs(q) <= maxSize && Math.abs(r) <= maxSize && Math.abs(getS()) <= maxSize;
    }

    @Override
    public int hashCode() {
        return 31 * q + r;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof HexPosition hex)) return false;
        return q == hex.q && r == hex.r;
    }

    @Override
    public String toString() {
        return String.format("HexPosition(q=%d, r=%d, s=%d)", q, r, getS());
    }
}