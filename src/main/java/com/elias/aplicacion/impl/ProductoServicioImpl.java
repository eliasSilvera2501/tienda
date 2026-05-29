package com.elias.aplicacion.impl;

import java.util.List;

import com.elias.aplicacion.ProductoServicio;
import com.elias.dominio.Producto;
import com.elias.dominio.repositorio.ProductoRepositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class ProductoServicioImpl implements ProductoServicio{
    
    @Inject
    private ProductoRepositorio productoRepositorio;

    @Override
    public List<Producto> obtenerTodos(){
        
        return productoRepositorio.obtenerTodos();
    }

    @Override
    public Producto obtenerPorId(Long id){

        if(existeProducto(id)){
            return productoRepositorio.obtenerPorId(id);
        }

        throw new IllegalArgumentException("El producto con ese id no existe");
    }

    @Override
    public Producto crear(Producto producto){
        return productoRepositorio.guardar(producto);
    }

    @Override
    public Producto actualizar(Long id, Producto producto){
        if(existeProducto(id)){
            Producto prod = productoRepositorio.obtenerPorId(id);
            if(producto.getNombre()!=null){ prod.setNombre(producto.getNombre());}
            if (producto.getPrecio()>0){prod.setPrecio(producto.getPrecio());}
            if(producto.getStock()!=null){prod.setStock(producto.getStock());}

            return productoRepositorio.actualizar(prod);
        }

        throw new IllegalArgumentException("No existe producto con esa id");
    }

    @Override
    public void eliminar(Long id){

        Producto p;
        if(!existeProducto(id)){
            throw new IllegalArgumentException("No existe producto con esa id");
        }else{
            p = productoRepositorio.obtenerPorId(id);
        }
        productoRepositorio.eliminar(p);
    }



    public boolean existeProducto(Long id){
        if(productoRepositorio.obtenerPorId(id) == null){
            return false;
        }

        return true;
    }

}
