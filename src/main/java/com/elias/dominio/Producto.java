package com.elias.dominio;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="productos")
public class Producto {
    

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private double precio;
    private int stock;

    public Producto(){}

    public Producto(String nombre, double precio, int stock){
        this.nombre=nombre;
        this.precio=precio;
        this.stock=stock;
    }

    public Long getId(){
        return id;
    }

    public String getNombre(){
        return nombre;
    }

    public double getPrecio(){
        return precio;
    }

    public int getStock(){
        return stock;
    }

    public void setNombre(String nombre){
        this.nombre=nombre;
    }

    public void setPrecio(double precio){
        this.precio=precio;
    }
    public void setStock(int stock){
        this.stock=stock;
    }

    public void setId(Long id){
        this.id=id;
    }
}
