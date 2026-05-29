package com.elias.infraestructura.persistencia;

import java.util.List;

import com.elias.dominio.Producto;
import com.elias.dominio.repositorio.ProductoRepositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class ProductoRepositorioImpl implements ProductoRepositorio{

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Producto> obtenerTodos(){
        return em.createQuery("SELECT p from Producto p", Producto.class).getResultList();
    }

    @Override
    public Producto obtenerPorId(Long id){
        return em.find(Producto.class,id);
    }

    @Override
    public Producto guardar(Producto producto){
        em.persist(producto);
        return producto;
    }

    @Override
    public Producto actualizar(Producto producto){
        
        em.merge(producto);
        return producto;
    }

    public void eliminar(Producto producto){
        
            em.remove(producto);
        
    }
}
