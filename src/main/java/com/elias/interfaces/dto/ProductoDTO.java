package com.elias.interfaces.dto;

public class ProductoDTO {

    private String nombre;
    private double precio;
    private Integer stock;


    public ProductoDTO(){}

    public ProductoDTO(String nombre, double precio, int stock){
        this.nombre= nombre;
        this.precio= precio;
        this.stock= stock;
    }

    public String getNombre(){
        return this.nombre;
    }

    public double getPrecio(){
        return this.precio;
    }

    public Integer getStock(){
        return this.stock;
    }

    public void setNombre(String nombre){
        this.nombre=nombre;
    }

    public void setPrecio(double precio){
        this.precio=precio;
    }

    public void setStock (int stock){
        this.stock=stock;
    }
    
}
