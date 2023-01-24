<html>
<body>
<span t:text="#{welcome.message}" />
<span t:if="${welcome.secondMessage == hello world2}">t:text="#{welcome.secondMessage}"</span>

<table>
<tr t:each="student: ${students}">
<td t:text="${student.id}" />
<td t:text="${student.name}" />
</tr>
</table>
</body>
</html>