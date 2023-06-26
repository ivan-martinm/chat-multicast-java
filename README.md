Chat cliente-servidor basado en multicast desarrollada en Java Swing.
Esta aplicación está desarrollada como proyecto del módulo de Programación de Servicios y Procesos, en combinación con contenidos vistos en el módulo de Desarrollo de interfaces, ambos cursados en el ciclo de Desarrollo de Aplicaciones Multiplataforma.

El objetivo del proyecto era el de demostrar los conocimientos aprendidos en el ciclo sobre programación concurrente, en materia de hilos (Threads) y aplicaciones de arquitectura Cliente-Servidor, creando una aplicación de Chat en la que varios clientes se conectan y envían mensajes por TCP a un mismo servidor, y éste, tras comprobar la validez, los reenvía a todos los clientes conectados a través de un socket multicast.

El proyecto debía cumplir además:
- Comprobación de nicks de los clientes para evitar duplicidad.
- Filtro de mensajes para evitar palabras prohibidas (en este caso una lista de marcas comerciales).
- Expulsión y prohibición de acceso a clientes que incumplan la norma anterior de forma reincidente (en este caso, tras 3 advertencias)
- Gestión correcta de las conexiones, sockets y recusos compartidos por los Threads.
- Interfaz gráfica opcional (en este caso, usando Swing y el editor de NetBeans por simplicidad de implementación).

Se incluyen además los ejecutables ya compilados para Servidor y Cliente (.jar). Para el correcto funcionamiento, sólo una instancia de Servidor debe estar activa, pudiendo haber activas tantas instancias de Cliente como se desee.
