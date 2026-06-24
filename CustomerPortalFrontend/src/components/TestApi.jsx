import axios from "axios";
import { useEffect } from "react";

function TestApi() {

  async function fetchUsers() {

    const response = await axios.get(
      "https://jsonplaceholder.typicode.com/users"
    );

    console.log(response.data);
  }

  useEffect(fetchUsers, []);

  return (
    <h1>Testing API</h1>
  );
}

export default TestApi;