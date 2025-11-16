<p align="center">
  <img src="assets/recordadora-logo.png" alt="Recordadora Logo" width="300">
</p>

<h1 align="center">🧙‍♂️ Recordadora</h1>

<p align="center">
  <b>Recordadora</b> es una extensión para <b>BurpSuite</b> inspirada en Harry Potter.<br>
  Como la recordadora de Neville, te ayuda a <b>no olvidar</b> ni perder el contexto de tus pruebas.
</p>

<h2>✨ Funcionalidades</h2>
<ul>
  <li>Guardar y restaurar <b>historial del Proxy</b></li>
  <li>Exportar e importar requests del <b>Repeater</b></li>
  <li>Persistir cargas del <b>Intruder</b></li>
  <li>Soporte para archivos <code>.log</code> para compartir sesiones</li>
  <li>Integración en menú contextual (clic derecho → enviar a Recordadora)</li>
  <li>Compatible con BurpSuite Community y Professional</li>
</ul>

<h2>📥 Instalación</h2>

<h3>Opción 1: Usar el <code>.jar</code> ya compilado</h3>
<ol>
  <li>Descarga la última versión desde <a href="https://github.com/Hackwarts12/Burp-Recordadora/releases">Releases</a>.</li>
  <li>Abre <b>BurpSuite</b> → <i>Extender</i> → <i>Extensions</i> → <i>Add</i>.</li>
  <li>Selecciona el archivo <code>recordadora.jar</code>.</li>
  <li>¡Listo! Verás <b>Recordadora</b> activa en Burp.</li>
</ol>

<h3>Opción 2: Compilar desde código fuente</h3>
<p>⚠️ Este repositorio <b>no incluye el código</b>, solo el <code>.jar</code> listo para usar.<br>
En futuras versiones se evaluará liberar el código para colaboración abierta.</p>

<h2>🔍 Comparativa con otras extensiones</h2>
<table>
  <thead>
    <tr>
      <th>Característica</th>
      <th>Logger++</th>
      <th>Flow</th>
      <th>Request Highlighter</th>
      <th>Recordadora</th>
    </tr>
  </thead>
  <tbody>
    <tr><td>Guarda tráfico del <b>Proxy</b></td><td>✅</td><td>✅</td><td>❌</td><td>✅</td></tr>
    <tr><td>Registro de <b>Repeater</b></td><td>❌</td><td>❌</td><td>❌</td><td>✅</td></tr>
    <tr><td>Registro de <b>Intruder</b></td><td>❌</td><td>❌</td><td>❌</td><td>✅</td></tr>
    <tr><td>Exporta a archivo (<code>.log</code>, <code>.csv</code>, etc.)</td><td>✅ (CSV/SQL)</td><td>✅</td><td>❌</td><td>✅ (formato <code>.log</code>)</td></tr>
    <tr><td><b>Importa</b> sesiones desde archivo</td><td>❌</td><td>❌</td><td>❌</td><td>✅</td></tr>
    <tr><td>Persistencia tras cerrar Burp</td><td>❌</td><td>❌</td><td>❌</td><td>✅</td></tr>
    <tr><td>Filtros avanzados en tiempo real</td><td>✅</td><td>✅</td><td>❌</td><td>🚧 Pend.</td></tr>
    <tr><td>Interfaz tipo tabla</td><td>✅</td><td>✅</td><td>✅</td><td>✅ (simpl.)</td></tr>
    <tr><td>Enfoque en <b>persistencia y restauración</b></td><td>❌</td><td>❌</td><td>❌</td><td>✅</td></tr>
  </tbody>
</table>

<p><b>Resumen:</b> Logger++ y Flow son útiles para análisis en vivo con filtros.<br>
<b>Recordadora</b> se centra en <b>guardar/restaurar</b> tu trabajo entre sesiones.</p>

<h2>📌 Estado</h2>
<p>Versión inicial – en desarrollo 🚀</p>
<ul>
  <li>Filtros en tiempo real (pendiente)</li>
  <li>Exportación adicional a CSV/JSON (pendiente)</li>
  <li>UI mejorada para grandes volúmenes (pendiente)</li>
</ul>

<h2>📝 Licencia</h2>
<p>Este proyecto se distribuye bajo la licencia incluida en el repositorio.</p>

<hr />

<p align="center">Hecho con 🪄 por <a href="https://github.com/Hackwarts12">Hackwarts12</a></p>
