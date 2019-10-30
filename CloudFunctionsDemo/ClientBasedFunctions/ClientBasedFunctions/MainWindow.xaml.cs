// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

using Microsoft.Research.SEAL;
using Newtonsoft.Json;
using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Collections.Generic;

namespace SEALAzureFuncClient
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        private HttpClient httpClient_ = new HttpClient();
        private Encryptor encryptor_ = null;
        private Decryptor decryptor_ = null;
        private BatchEncoder encoder_ = null;
        private string galoisKeys_ = null;
        private string relinKeys_ = null;
        private string sid_ = null;
        private Task t_ = null;

        /// <summary>
        /// Constructor for MainWindow
        /// </summary>
        public MainWindow()
        {
            InitializeComponent();

            // Initialize in background thread
            t_ = Task.Run(() =>
            {
                KeyGenerator keygen = new KeyGenerator(GlobalProperties.Context);

                // We set up SEAL in symmetric-key mode by creating the Encryptor with
                // the secret key instead of public key.
                encryptor_ = new Encryptor(GlobalProperties.Context, keygen.SecretKey);
                decryptor_ = new Decryptor(GlobalProperties.Context, keygen.SecretKey);
                encoder_ = new BatchEncoder(GlobalProperties.Context);

                galoisKeys_ = Utilities.GaloisKeysToBase64(keygen, CreateRotations());
                relinKeys_ = Utilities.RelinKeysToBase64(keygen);

                // Choose the Session ID
                RandomizeSID();
            });
        }

        /// <summary>
        /// Generate a random Session ID
        /// </summary>
        private void RandomizeSID()
        {
            // Choose the Session ID
            byte[] sidBytes = new byte[32];
            Random rnd = new Random();
            rnd.NextBytes(sidBytes);
            sid_ = Convert.ToBase64String(sidBytes);
        }

        /// <summary>
        /// Computes power-of-two step rotation step sizes in one direction only.
        /// This is important because normally SEAL would create rotations in both
        /// directions, which in this applications is not needed and would double
        /// the size of the GaloisKeys.
        /// </summary>
        /// <returns></returns>
        private List<int> CreateRotations()
        {
            // Create power-of-two rotations in one direction only
            List<int> rotCounts = new List<int>();
            for (int i = (int)encoder_.SlotCount / GlobalProperties.MatrixSizeMax; i < (int)encoder_.SlotCount / 2; i *= 2)
            {
                rotCounts.Add(i);
            }
            rotCounts.Add(0);
            return rotCounts;
        }

        /// <summary>
        /// Add button clicked
        /// </summary>
        private async void OnAddClicked(object sender, RoutedEventArgs e)
        {
            await PerformMatrixOperation(sid_, "Addition", GlobalProperties.Codes.Addition);
        }

        /// <summary>
        /// Subtract button clicked
        /// </summary>
        private async void OnSubtractClicked(object sender, RoutedEventArgs e)
        {
            await PerformMatrixOperation(sid_, "Subtraction", GlobalProperties.Codes.Subtraction);
        }

        /// <summary>
        /// Multiply button clicked
        /// </summary>
        private async void OnMultiplyClicked(object sender, RoutedEventArgs e)
        {
            await PerformMatrixMultiplication(sid_, GlobalProperties.Codes.MatrixVectorProduct);
        }

        /// <summary>
        /// Get an Uri by appending the given function to a base Uri
        /// </summary>
        /// <param name="function">Function to append</param>
        private Uri GetUri(string function, string code)
        {
            Uri baseUri = null;
            if (!HostAddress.Text.StartsWith("http", StringComparison.InvariantCultureIgnoreCase))
            {
                baseUri = new Uri($"http://{HostAddress.Text}");
            }
            else
            {
                baseUri = new Uri(HostAddress.Text.Trim());
            }

            Uri functionUri = new Uri(baseUri, $"api/{function}?code={code}");
            return functionUri;
        }

        /// <summary>
        /// Ask server if a public key of the given type is present with the given Session ID
        /// </summary>
        /// <param name="sid">Session ID</param>
        /// <param name="keyType">Type of key</param>
        /// <param name="code">Azure function key</param>
        private async Task<bool> QueryPublicKeys(string sid, string keyType, string code)
        {
            // Query keys for given sid 
            Uri function = GetUri("PublicKeysQuery", code);
            string json = $"{{ \"sid\": \"{sid}\", \"type\": \"{keyType}\" }}";
            HttpContent content = new StringContent(json, Encoding.UTF8);
            HttpResponseMessage response;

            try
            {
                response = await httpClient_.PostAsync(function, content);
            }
            catch (HttpRequestException ex)
            {
                Log($"Exception during request: {ex.ToString()}");
                return false;
            }
            Log($"PublicKeysQuery (type {keyType}) response: {response.StatusCode}");

            return response.IsSuccessStatusCode;
        }

        /// <summary>
        /// Upload public keys of a given type to the Azure Functions server.
        /// 
        /// RelinKeys and GaloisKeys are needed in order to reduce size of the transmitted
        /// ciphertexts (through relinearization), or to perform element rotations needed
        /// for multiplication.
        /// Relinearization reduces ciphertext size, which means fewer data is transmitted
        /// between the client and the server.
        /// </summary>
        /// <param name="sid">Session ID</param>
        /// <param name="code">Azure function key</param>
        private async Task<bool> UploadPublicKeys(
            string sid, GlobalProperties.PublicKeyType pkType, string code)
        {
            string b64key = null;
            string keyType = null;
            switch (pkType)
            {
                case GlobalProperties.PublicKeyType.GaloisKeys:
                    keyType = "GaloisKeys";
                    b64key = galoisKeys_;
                    break;

                case GlobalProperties.PublicKeyType.RelinKeys:
                    keyType = "RelinKeys";
                    b64key = relinKeys_;
                    break;

                default:
                    Log($"Invalid key type");
                    return false;
            };

            // Upload RelinKeys for given sid 
            Uri function = GetUri("PublicKeysUpload", code);
            string json = $"{{ \"sid\": \"{sid}\", \"type\": \"{keyType}\", \"key\": \"{b64key}\" }}";
            HttpContent content = new StringContent(json, Encoding.UTF8);
            HttpResponseMessage response;

            double kbs = b64key.Length / 1024.0;
            Log($"Sending {kbs} KB of {keyType} data to Azure Function");

            try
            {
                response = await httpClient_.PostAsync(function, content);
            }
            catch (HttpRequestException ex)
            {
                Log($"Exception during request: {ex.ToString()}");
                return false;
            }
            Log($"PublicKeysUpload (type {keyType}) response: {response.StatusCode}");

            return response.IsSuccessStatusCode;
        }

        /// <summary>
        /// Delete given public key type for the given SID from server
        /// </summary>
        /// <param name="sid">Session ID</param>
        /// <param name="keyType">key type</param>
        /// <param name="code">Azure function key</param>
        /// <returns></returns>
        private async Task<bool> DeletePublicKeys(string sid, string keyType, string code)
        {
            // Delete RelinKeys for given sid 
            Uri function = GetUri("PublicKeysDelete", code);
            string json = $"{{ \"sid\": \"{sid}\", \"type\": \"{keyType}\" }}";
            HttpContent content = new StringContent(json, Encoding.UTF8);
            HttpResponseMessage response;

            try
            {
                response = await httpClient_.PostAsync(function, content);
            }
            catch (HttpRequestException ex)
            {
                Log($"Exception during request: {ex.ToString()}");
                return false;
            }
            Log($"PublicKeysDelete (type {keyType}) response: {response.StatusCode}");

            return response.IsSuccessStatusCode;
        }

        /// <summary>
        /// Perform a Matrix operation (add / subtract)
        /// </summary>
        /// <param name="sid">Session ID</param>
        /// <param name="operation">Operation to perform</param>
        /// <param name="code">Azure Function key</param>
        private async Task PerformMatrixOperation(string sid, string operation, string code)
        {
            ClearLog();
            Log($"Session ID: {sid}");

            MatrixData mda = MatrixA.Matrix.DataContext as MatrixData;
            MatrixData mdb = MatrixB.Matrix.DataContext as MatrixData;
            Plaintext plaina = Utilities.MatrixToPlaintext(Utilities.MatrixDataToMatrix(mda), encoder_);
            Plaintext plainb = Utilities.MatrixToPlaintext(Utilities.MatrixDataToMatrix(mdb), encoder_);

            Plaintext plain = await PerformOperation(sid, operation, code, plaina, plainb);
            if (null == plain)
            {
                OperationResult.MatrixTitle = $"Error executing {operation}";
                OperationResult.ClearMatrix();
                return;
            }

            int rows = mda.DataView.Table.Rows.Count;
            int cols = mda.DataView.Table.Columns.Count;
            int[,] matresult = Utilities.PlaintextToMatrix(plain, rows, cols, encoder_);

            OperationResult.MatrixTitle = "Result";
            OperationResult.InitMatrix(matresult);
        }

        /// <summary>
        /// Perform a Matrix multiplication.
        /// 
        /// Please refer to the file MATRIXMULTIPLICATION.md for an explanation of
        /// how matrix multiplication is implemented in this application.
        /// </summary>
        private async Task PerformMatrixMultiplication(string sid, string code)
        {
            ClearLog();
            Log($"Session ID: {sid}");

            // Query RelinKeys with given sid 
            if (!await QueryPublicKeys(sid, "RelinKeys", GlobalProperties.Codes.PublicKeysQuery))
            {
                // Upload RelinKeys if needed
                if (!await UploadPublicKeys(
                    sid, GlobalProperties.PublicKeyType.RelinKeys,
                    GlobalProperties.Codes.PublicKeysUpload))
                {
                    Log($"RelinKeys upload failed");
                    return;
                }
            }

            // Query GaloisKeys with given sid 
            if (!await QueryPublicKeys(sid, "GaloisKeys", GlobalProperties.Codes.PublicKeysQuery))
            {
                // Upload GaloisKeys if needed
                if (!await UploadPublicKeys(
                    sid, GlobalProperties.PublicKeyType.GaloisKeys,
                    GlobalProperties.Codes.PublicKeysUpload))
                {
                    Log($"GaloisKeys upload failed");
                    return;
                }
            }

            MatrixData mda = MatrixA.Matrix.DataContext as MatrixData;
            MatrixData mdb = MatrixB.Matrix.DataContext as MatrixData;

            int[,] matrixa = Utilities.MatrixDataToMatrix(mda);
            int[,] matrixb = Utilities.MatrixDataToMatrix(mdb);
            int marows = matrixa.GetLength(dimension: 0);
            int macols = matrixa.GetLength(dimension: 1);
            int mbrows = matrixb.GetLength(dimension: 0);
            int mbcols = matrixb.GetLength(dimension: 1);
            if (marows < 1 || macols < 1 || mbrows < 1 || mbcols < 1 || macols != mbrows)
            {
                throw new InvalidOperationException("matrices are incompatible for multiplication");
            }

            bool transposed = false;
            if (Math.Max(mbrows, mbcols) < Math.Max(marows, macols))
            {
                transposed = true;
                Log("Computing on transposes for improved performance");
                (matrixa, matrixb) = (Utilities.Transpose(matrixb), Utilities.Transpose(matrixa));
                (marows, macols, mbrows, mbcols) = (mbcols, mbrows, macols, marows);
            }

            // We use a power of two dimension always
            int dimension = Utilities.FindNextPowerOfTwo((ulong)Math.Max(marows, macols));

            int[,] paddedMatrixa = new int[dimension, dimension];
            for (int r = 0; r < marows; r++)
            {
                for (int c = 0; c < macols; c++)
                {
                    paddedMatrixa[r, c] = matrixa[r, c];
                }
            }

            // Create a matrix whose rows are generalized diagonals of matrix A
            int[,] cyclicDiagsMatrix = new int[dimension, dimension];
            for (int r = 0; r < dimension; r++)
            {
                for (int c = 0; c < dimension; c++)
                {
                    cyclicDiagsMatrix[r, c] = paddedMatrixa[c, (c + r) % dimension];
                }
            }

            // Make cyclicDiagsMatrix into a List of plaintexts
            List<Plaintext> cdmPlains = Utilities.RowsToPlaintexts(cyclicDiagsMatrix, mbcols, encoder_);

            // Make matrixb into row-major set of plaintexts
            int[,] paddedMatrixb = new int[dimension, mbcols];
            for (int r = 0; r < mbrows; r++)
            {
                for (int c = 0; c < mbcols; c++)
                {
                    paddedMatrixb[r, c] = matrixb[r, c];
                }
            }
            Plaintext matrixbPlain = Utilities.MatrixToTwistedPlaintext(paddedMatrixb, encoder_);

            Plaintext plainResult = await PerformMatrixProductOperation(sid, code, cdmPlains, matrixbPlain);
            if (null == plainResult)
            {
                OperationResult.MatrixTitle = "Error performing matrix product";
                OperationResult.ClearMatrix();

                return;
            }

            int[,] wideResult = new int[dimension, mbcols];
            wideResult = Utilities.PlaintextToMatrix(plainResult, dimension, mbcols, encoder_);

            if (transposed)
            {
                wideResult = Utilities.Transpose(wideResult);
                (marows, macols, mbrows, mbcols) = (mbcols, mbrows, macols, marows);
            }

            int[,] result = new int[marows, mbcols];
            for (int r = 0; r < marows; r++)
            {
                for (int c = 0; c < mbcols; c++)
                {
                    result[r, c] = wideResult[r, c];
                }
            }

            OperationResult.MatrixTitle = "Result";
            OperationResult.InitMatrix(result);
        }

        /// <summary>
        /// Perform a matrix operation by invocating an Azure Function
        /// </summary>
        /// <param name="sid">Session ID</param>
        /// <param name="operation">Operation to perform</param>
        /// <param name="code">Azure Key needed to execute Azure Function in Azure</param>
        /// <param name="ciphera">Ciphertext containing a codified matrix</param>
        /// <param name="cipherb">Ciphertext containing a second codified matrix</param>
        /// <returns>Plaintext with the result of the operation</returns>
        private async Task<Plaintext> PerformOperation(string sid, string operation, string code, Plaintext plaina, Plaintext plainb)
        {
            string b64a = Utilities.EncryptToBase64(plaina, encryptor_);
            string b64b = Utilities.EncryptToBase64(plainb, encryptor_);
            string json = $"{{ \"sid\": \"{sid}\", \"matrixa\": \"{b64a}\", \"matrixb\": \"{b64b}\" }}";

            double kbs = (b64a.Length + b64b.Length) / 1024.0;
            Log($"Matrix A:\n{b64a}");
            Log($"Matrix B:\n{b64b}");
            Log($"Sending {kbs} KB of ciphertext data to Azure Function");

            Uri function = GetUri(operation, code);
            HttpContent content = new StringContent(json, Encoding.UTF8);
            HttpResponseMessage response;

            try
            {
                response = await httpClient_.PostAsync(function, content);
            }
            catch (HttpRequestException ex)
            {
                Log($"Exception during request: {ex.ToString()}");
                return null;
            }
            if (!response.IsSuccessStatusCode)
            {
                Log($"Function call failed with status code: {response.StatusCode}");
                return null;
            }

            string responseStr = await response.Content.ReadAsStringAsync();
            dynamic deserialized = JsonConvert.DeserializeObject(responseStr);
            string resultb64 = deserialized?.result;

            Log($"Result:\n{resultb64}");

            Ciphertext result = Utilities.Base64ToCiphertext(resultb64, GlobalProperties.Context);
            Plaintext plain = new Plaintext();
            Log("Noise budget: " + decryptor_.InvariantNoiseBudget(result) + " bits");
            decryptor_.Decrypt(result, plain);

            return plain;
        }

        /// <summary>
        /// Perform matrix product operation by invoking an Azure Function
        /// </summary>
        /// <param name="sid">Session ID</param>
        /// <param name="code">Azure Key needed to execute Azure Function in Azure</param>
        /// <param name="matrixa">Ciphertext enumeration containing a codified first matrix</param>
        /// <param name="matrixb">Ciphertext containing a codified second matrix</param>
        /// <returns>Plaintext with the result of the operation</returns>
        private async Task<Plaintext> PerformMatrixProductOperation(string sid, string code, IEnumerable<Plaintext> matrixa, Plaintext matrixb)
        {
            string b64matrixa = Utilities.EncryptToBase64(matrixa, encryptor_);
            string b64matrixb = Utilities.EncryptToBase64(matrixb, encryptor_);
            string json = $"{{ \"sid\": \"{sid}\", \"matrixa\": \"{b64matrixa}\", \"matrixb\": \"{b64matrixb}\" }}";

            double kbs = (b64matrixa.Length + b64matrixb.Length) / 1024.0;
            Log($"Matrix A:\n{b64matrixa}");
            Log($"Matrix B:\n{b64matrixb}");
            Log($"Sending {kbs} KB of ciphertext data to Azure Function");

            Uri function = GetUri("MatrixProduct", code);
            HttpContent content = new StringContent(json, Encoding.UTF8);
            HttpResponseMessage response;

            try
            {
                response = await httpClient_.PostAsync(function, content);
            }
            catch (HttpRequestException ex)
            {
                Log($"Exception during request: {ex.ToString()}");
                return null;
            }
            if (!response.IsSuccessStatusCode)
            {
                Log($"Function call failed with status code: {response.StatusCode}");
                return null;
            }

            string responseStr = await response.Content.ReadAsStringAsync();
            dynamic deserialized = JsonConvert.DeserializeObject(responseStr);
            string resultb64 = deserialized?.result;

            Log($"Result:\n{resultb64}");

            Ciphertext resultCipher = Utilities.Base64ToCiphertext(resultb64, GlobalProperties.Context);
            Plaintext result = new Plaintext();
            Log("Noise budget: " + decryptor_.InvariantNoiseBudget(resultCipher) + " bits");
            decryptor_.Decrypt(resultCipher, result);

            return result;
        }

        /// <summary>
        /// Handle event raised with Matrix A changes size
        /// </summary>
        private void OnMatrixASizeChanged(object sender, EventArgs e)
        {
            OnMatrixSizeChanged();
        }

        /// <summary>
        /// Handle event raised when Matrix B changes size
        /// </summary>
        private void OnMatrixBSizeChanged(object sender, EventArgs e)
        {
            OnMatrixSizeChanged();
        }

        /// <summary>
        /// Enable / Disable operation buttons depending on the sizes of Matrix A and Matrix B
        /// </summary>
        private void OnMatrixSizeChanged()
        {
            int rowsa = MatrixA.MatrixSize.Item1;
            int colsa = MatrixA.MatrixSize.Item2;
            int rowsb = MatrixB.MatrixSize.Item1;
            int colsb = MatrixB.MatrixSize.Item2;

            if (rowsa == rowsb && colsa == colsb)
            {
                AddButton.IsEnabled = true;
                SubButton.IsEnabled = true;
            }
            else
            {
                AddButton.IsEnabled = false;
                SubButton.IsEnabled = false;
            }

            if (colsa == rowsb)
            {
                MulButton.IsEnabled = true;
            }
            else
            {
                MulButton.IsEnabled = false;
            }

            // If we had a result it is not valid anymore, so the results are cleared.
            OperationResult.ClearMatrix();
        }

        /// <summary>
        /// Delete the keys that we sent to the server when closing the application.
        /// </summary>
        /// <param name="sender">Event sender</param>
        /// <param name="e">Event arguments</param>
        private async void OnMainWindowClose(object sender, EventArgs e)
        {
            // Delete GaloisKeys from server for given Session ID
            await DeletePublicKeys(sid_, "GaloisKeys", GlobalProperties.Codes.PublicKeysDelete);

            // Delete RelinKeys from server for given Session ID
            await DeletePublicKeys(sid_, "RelinKeys", GlobalProperties.Codes.PublicKeysDelete);
        }

        /// <summary>
        /// Log an entry by appending the the Log textbox
        /// </summary>
        /// <param name="entry">Entry to log</param>
        private void Log(string entry)
        {
            LogTextBox.AppendText($"{entry}\n");
            LogTextBox.ScrollToEnd();
        }

        /// <summary>
        /// Clears log text box
        /// </summary>
        private void ClearLog()
        {
            LogTextBox.Clear();
        }
    }
}
