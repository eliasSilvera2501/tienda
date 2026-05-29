package com.elias.aplicacion;

import java.util.List;

import com.elias.dominio.Producto;
import com.elias.interfaces.dto.ProductoResponseDTO;

public interface ProductoServicio {


    List<Producto> obtenerTodos();
    Producto obtenerPorId(Long id);
    Producto  crear(Producto producto);
    Producto actualizar(Long id, Producto producto);
    void eliminar(Long id);
    boolean existeProducto(Long id);
    
}
