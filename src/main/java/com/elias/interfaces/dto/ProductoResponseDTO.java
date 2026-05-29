package com.elias.interfaces.dto;

public class ProductoResponseDTO {

    private Long id;
    private String nombre;
    private double precio;
    private int stock;


    public ProductoResponseDTO(){}

    public ProductoResponseDTO(Long id, String nombre, double precio, int stock){
        this.nombre= nombre;
        this.precio= precio;
        this.stock= stock;
        this.id= id;
    }

    public Long getId(){
        return this.id;
    }

    public void setId(Long id){
        this.id=id;
    }
    public String getNombre(){
        return this.nombre;
    }

    public double getPrecio(){
        return this.precio;
    }

    public int getStock(){
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
