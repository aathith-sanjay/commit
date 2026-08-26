import axios from 'axios'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL
    ? `${import.meta.env.VITE_API_BASE_URL}/api/v1`
    : '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

// Attach JWT from localStorage to every request
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('commit_token')
  if (token) config.headers['Authorization'] = `Bearer ${token}`
  return config
})

// On 401, clear token and redirect to login
client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('commit_token')
      localStorage.removeItem('commit_user')
      window.location.href = `${import.meta.env.VITE_BASE_PATH ?? '/'}login`
    }
    return Promise.reject(err)
  }
)

export default client
