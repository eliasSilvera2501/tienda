package com.elias.dominio.repositorio;

import java.util.List;

import com.elias.dominio.Producto;

public interface ProductoRepositorio {

    List<Producto> obtenerTodos();
    Producto obtenerPorId(Long id);
    Producto guardar(Producto producto);
    Producto actualizar(Producto producto);
    void eliminar (Producto producto);

}
